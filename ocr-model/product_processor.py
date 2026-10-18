import csv
import re
import unicodedata
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

from retrieval import (
    DualFieldBM25Retriever,
    FuzzyWRatioRetriever,
    SearchDocument,
)


BASE_DIR = Path(__file__).resolve().parent

INGREDIENTS_FILE = BASE_DIR / "Ingredients_rows.csv"
NON_FOOD_FILE = BASE_DIR / "non_food_types.csv"
LOCAL_TIMEZONE = ZoneInfo("Asia/Ho_Chi_Minh")
FUZZY_AUTO_MATCH_THRESHOLD = 0.85
BM25_MIN_SCORE = 5.0


# ============================================================
# NORMALIZATION
# ============================================================

def normalize_text_preserve_accents(text: str) -> str:
    text = text.lower().strip()

    text = unicodedata.normalize("NFC", text)

    text = "".join(
        char if char.isalnum() else " "
        for char in text
    )

    return re.sub(r"\s+", " ", text).strip()


def normalize_text(text: str) -> str:
    text = normalize_text_preserve_accents(text)

    # NFD does not convert đ
    text = text.replace("đ", "d")

    text = unicodedata.normalize("NFD", text)

    text = "".join(
        char
        for char in text
        if unicodedata.category(char) != "Mn"
    )

    text = re.sub(
        r"[^a-z0-9]+",
        " ",
        text,
    )

    text = re.sub(
        r"\s+",
        " ",
        text,
    ).strip()

    return text


def remove_package_size(text: str) -> str:
    # 500g, 190ml, 1kg, 22cm, 1L...
    text = re.sub(
        r"\b\d+(?:[.,]\d+)?\s*"
        r"(kg|g|gam|gram|ml|l|cm)\b",
        " ",
        text,
    )

    return re.sub(r"\s+", " ", text).strip()


def normalize_for_accentless_lookup(text: str) -> str:
    """
    Remove obvious package size from the name before exact lookup.

    Examples:

        "Bún tươi 500g"
             ->
        "bun tuoi"

        "Chà bông giòn 20g"
             ->
        "cha bong gion"
    """

    text = normalize_text(text)

    return remove_package_size(text)


def normalize_for_accented_lookup(text: str) -> str:
    return remove_package_size(
        normalize_text_preserve_accents(text)
    )


def has_diacritics(text: str) -> bool:
    normalized = normalize_text_preserve_accents(text)
    return normalize_text(normalized) != normalized


# ============================================================
# LOAD MASTER DATA
# ============================================================

def load_ingredients():
    by_id = {}
    by_name = {}

    with open(
        INGREDIENTS_FILE,
        encoding="utf-8-sig",
        newline="",
    ) as file:
        reader = csv.DictReader(file)

        for row in reader:
            ingredient_id = row["id"].strip()
            name = row["name"].strip()

            record = {
                "id": ingredient_id,
                "name": name,
            }

            by_id[ingredient_id] = record

            normalized_name = normalize_text(name)

            by_name[normalized_name] = record

    return by_id, by_name


def load_non_food():
    by_id = {}
    by_name = {}

    with open(
        NON_FOOD_FILE,
        encoding="utf-8-sig",
        newline="",
    ) as file:
        reader = csv.DictReader(file)

        for row in reader:
            non_food_id = row["id"].strip()
            name = row["name"].strip()

            record = {
                "id": non_food_id,
                "name": name,
                "category": row["category"].strip(),
            }

            by_id[non_food_id] = record

            normalized_name = normalize_text(name)

            by_name[normalized_name] = record

    return by_id, by_name


INGREDIENTS_BY_ID, INGREDIENTS_BY_NAME = load_ingredients()

NON_FOOD_BY_ID, NON_FOOD_BY_NAME = load_non_food()


# ============================================================
# RETRIEVAL INDEX
# ============================================================

def tokenize_without_accents(text: str) -> list[str]:
    return normalize_for_accentless_lookup(text).split()


def tokenize_with_accents(text: str) -> list[str]:
    return normalize_for_accented_lookup(text).split()


FOOD_DOCUMENTS = [
    SearchDocument(
        id=ingredient["id"],
        name=ingredient["name"],
        product_type="food",
        metadata={},
    )
    for ingredient in INGREDIENTS_BY_ID.values()
]

NON_FOOD_DOCUMENTS = [
    SearchDocument(
        id=non_food["id"],
        name=non_food["name"],
        product_type="non_food",
        metadata={"category": non_food["category"]},
    )
    for non_food in NON_FOOD_BY_ID.values()
]

SEARCH_DOCUMENTS = FOOD_DOCUMENTS + NON_FOOD_DOCUMENTS


DUAL_BM25_RETRIEVER = DualFieldBM25Retriever(
    SEARCH_DOCUMENTS,
    accented_tokenizer=tokenize_with_accents,
    accentless_tokenizer=tokenize_without_accents,
    has_diacritics=has_diacritics,
)

# Dual BM25 is the only retriever initialized by the production API.
PRODUCT_RETRIEVER = DUAL_BM25_RETRIEVER


def make_retriever(documents):
    return DualFieldBM25Retriever(
        documents,
        accented_tokenizer=tokenize_with_accents,
        accentless_tokenizer=tokenize_without_accents,
        has_diacritics=has_diacritics,
    )


# New OCR classification checks non-foods first, then searches only food.
# Keep the combined retriever above for the legacy retrieval API.
NON_FOOD_RETRIEVER = make_retriever(NON_FOOD_DOCUMENTS)
FOOD_RETRIEVER = make_retriever(FOOD_DOCUMENTS)
FOOD_FUZZY_RETRIEVER = FuzzyWRatioRetriever(
    FOOD_DOCUMENTS,
    normalizer=normalize_for_accentless_lookup,
)


EXACT_DOCUMENTS = {}
for document in SEARCH_DOCUMENTS:
    key = normalize_for_accented_lookup(document.name)
    EXACT_DOCUMENTS.setdefault(key, []).append(document)

EXACT_FOOD_DOCUMENTS = {}
for document in FOOD_DOCUMENTS:
    EXACT_FOOD_DOCUMENTS.setdefault(
        normalize_for_accented_lookup(document.name), []
    ).append(document)

EXACT_NON_FOOD_DOCUMENTS = {}
for document in NON_FOOD_DOCUMENTS:
    EXACT_NON_FOOD_DOCUMENTS.setdefault(
        normalize_for_accented_lookup(document.name), []
    ).append(document)


def current_local_date() -> str:
    return datetime.now(LOCAL_TIMEZONE).date().isoformat()


def parse_qty_display(value) -> int | float | None:
    """Coerce Azure Quantity to number for backend POST /scans (root fix)."""
    if value is None:
        return None
    if isinstance(value, (int, float)):
        return value
    text = str(value).strip().replace(",", ".")
    if not text:
        return None
    try:
        number = float(text)
    except ValueError:
        return None
    if number.is_integer():
        return int(number)
    return number


def find_top_document(raw_name: str) -> SearchDocument | None:
    """Return an exact database match or Dual BM25's top-1 result."""

    lookup_name = normalize_for_accented_lookup(raw_name)
    exact_documents = EXACT_DOCUMENTS.get(lookup_name, [])

    if len(exact_documents) == 1:
        return exact_documents[0]

    results = PRODUCT_RETRIEVER.search(
        lookup_name,
        top_k=1,
    )
    return results[0].document if results else None


def build_receipt_product(
    item: dict,
    document: SearchDocument,
    current_date: str,
) -> dict:
    """Build the public JSON shape expected by the client application."""

    return {
        "name": document.name,
        "ingredient_id": document.id,
        "qty_display": parse_qty_display(item.get("quantity")),
        # The receipt unit is authoritative. Never use CSV base_unit here.
        "unit_display": item.get("unit"),
        "purchased_at": current_date,
        "expiry_date": current_date,
    }


# ============================================================
# PRODUCT PROCESSOR
# ============================================================

def process_product(
    item: dict,
    current_date: str | None = None,
) -> dict | None:
    document = find_top_document(item["name"])

    if document is None or document.product_type != "food":
        return None

    return build_receipt_product(
        item,
        document,
        current_date or current_local_date(),
    )


def process_products(items: list[dict]) -> dict:
    date_value = current_local_date()
    matched_items = []
    ignored_non_food_count = 0
    unmatched_item_count = 0

    for item in items:
        document = find_top_document(item["name"])

        if document is None:
            unmatched_item_count += 1
            continue

        if document.product_type != "food":
            ignored_non_food_count += 1
            continue

        matched_items.append(
            build_receipt_product(item, document, date_value)
        )

    return {
        "items": matched_items,
        "ignored_non_food_count": ignored_non_food_count,
        "unmatched_item_count": unmatched_item_count,
    }


def classify_ocr_items(items: list[dict], purchased_at: str | None = None) -> dict:
    """Preserve every OCR line and its measurements for user review.

    The legacy process_products shape is retained for old clients. New OCR
    routes use this response and never invent an expiry date.
    """
    food_items = []
    for item in items:
        raw_name = (item.get("raw_name") or "").strip()
        if not raw_name:
            continue

        # First classify non-foods. Exact catalog phrases and obvious cookware
        # wording catch OCR noise such as "Nỗi inox" (Nồi inox). This decision
        # is intentionally silent in the user-facing item list.
        if is_non_food_item(raw_name):
            continue

        exact = EXACT_FOOD_DOCUMENTS.get(normalize_for_accented_lookup(raw_name), [])
        if item.get("source_type") == "product":
            # Labels contain a brand and OCR can distort one syllable. BM25
            # alone may rank a one-word catalogue entry such as "Lê" over
            # the multi-word product. Reward coverage of the whole label.
            candidates = sorted(
                (result for result in FOOD_RETRIEVER.search(raw_name, top_k=30)
                 if result.query_coverage >= 0.4 and result.document_coverage >= 0.5),
                key=lambda result: (result.score * result.query_coverage,
                                    result.document_coverage),
                reverse=True,
            )[:2]
        else:
            candidates = FOOD_RETRIEVER.search(raw_name, top_k=3)
        ranked_candidates = list(candidates)
        # Add one spelling-tolerant food suggestion. It can help the user find
        # a one-character OCR correction (e.g. "Cá lãng" -> "Cá lăng").
        fuzzy_candidates = FOOD_FUZZY_RETRIEVER.search(raw_name, top_k=2)
        fuzzy_candidate = fuzzy_candidates[0] if fuzzy_candidates else None
        # Auto-map based on the top candidate crossing its score threshold.
        # Do not require it to beat runner-up candidates by a margin.
        fuzzy_suggestion = (
            fuzzy_candidate
            if fuzzy_candidate and fuzzy_candidate.score >= FUZZY_AUTO_MATCH_THRESHOLD
            else None
        )
        fuzzy_auto_mapped = bool(
            fuzzy_suggestion
            and len(exact) != 1
        )
        if fuzzy_suggestion and not (len(exact) == 1):
            candidates = [fuzzy_suggestion] + [
                result for result in candidates
                if result.document.id != fuzzy_suggestion.document.id
            ]
        elif fuzzy_candidate and fuzzy_candidate.score >= 0.8:
            if all(result.document.id != fuzzy_candidate.document.id
                   for result in candidates):
                candidates.append(fuzzy_candidate)
        top = exact[0] if len(exact) == 1 else (ranked_candidates[0].document if ranked_candidates else None)
        top_score = ranked_candidates[0].score if ranked_candidates else 0.0
        bm25_confident = bool(
            top_score >= BM25_MIN_SCORE
        )
        confident = len(exact) == 1 or bm25_confident or fuzzy_auto_mapped
        mapped_document = (
            exact[0] if len(exact) == 1 else
            top if bm25_confident else
            fuzzy_suggestion.document if fuzzy_auto_mapped else
            top
        )
        if mapped_document:
            candidates.sort(key=lambda result: result.document.id != mapped_document.id)
        status = "matched" if mapped_document and confident else "needs_review"
        accepted = bool(mapped_document and confident)
        display_suggestion = (
            mapped_document if confident else
            fuzzy_suggestion.document if fuzzy_suggestion else
            top
        )
        row = {
            # `name` is always a canonical food catalogue name when we have a
            # plausible match. Raw OCR text is kept separately for correction.
            # Ambiguous suggestions have no ingredient_id until user selection.
            "name": display_suggestion.name if display_suggestion else raw_name,
            "raw_name": raw_name,
            "ingredient_id": mapped_document.id if accepted and mapped_document else None,
            "qty_display": item.get("quantity"),
            "unit_display": item.get("unit"),
            "unit_price_vnd": item.get("unit_price_vnd"),
            "price_vnd": item.get("price_vnd"),
            "purchased_at": purchased_at,
            "expiry_date": item.get("expiry_date"),
            "source_type": item.get("source_type", "receipt"),
            "match_status": status,
            "candidates": [
                {"name": result.document.name,
                 "ingredient_id": result.document.id if result.document.product_type == "food" else None,
                 "type": result.document.product_type}
                for result in candidates
            ],
        }
        food_items.append(row)
    return {"items": food_items, "excluded_items": []}


def is_non_food_item(raw_name: str) -> bool:
    """Silently exclude confidently identified household/non-food receipt rows."""
    normalized = normalize_for_accented_lookup(raw_name)
    exact = EXACT_NON_FOOD_DOCUMENTS.get(normalized, [])
    if len(exact) == 1:
        return True

    accentless = normalize_for_accentless_lookup(raw_name)
    query_tokens = accentless.split()

    # Common receipt names include a brand/size after the catalog phrase.
    # Require the full multi-word catalog phrase (or a distinctive one-word
    # kitchen item) so generic terms such as "nước" do not suppress food.
    for document in NON_FOOD_DOCUMENTS:
        phrase = normalize_for_accentless_lookup(document.name).split()
        if not phrase:
            continue
        if len(phrase) >= 2 and _contains_token_phrase(query_tokens, phrase):
            return True
        if (len(phrase) == 1 and phrase[0] in {
            "noi", "chao", "keo", "chen", "dia", "ly", "to"
        } and phrase[0] in query_tokens
                and ("inox" in query_tokens or "bo" in query_tokens
                     or len(query_tokens) == 1)):
            return True

    # OCR often confuses the tone in "nồi". Inox plus a kitchenware word is
    # strong enough to classify the row as household goods.
    kitchenware = {
        "noi", "chao", "dao", "keo", "chen", "dia", "ly", "to",
        "muong", "va", "thia", "nia", "dua",
    }
    if "inox" in query_tokens and kitchenware.intersection(query_tokens):
        return True
    if {"bo", "va", "muong"}.issubset(query_tokens):
        return True

    # Fuzzy catalog fallback is isolated from the food index. It is used only
    # with strong evidence and a clear lead over the next non-food candidate.
    candidates = NON_FOOD_RETRIEVER.search(raw_name, top_k=2)
    if not candidates:
        return False
    top = candidates[0]
    second_score = candidates[1].score if len(candidates) > 1 else 0.0
    return (
        top.score >= 5.0
        and top.document_coverage >= 0.65
        and top.query_coverage >= 0.4
        and (second_score == 0 or top.score >= second_score * 1.15)
    )


def _contains_token_phrase(tokens: list[str], phrase: list[str]) -> bool:
    return any(tokens[index:index + len(phrase)] == phrase
               for index in range(len(tokens) - len(phrase) + 1))
