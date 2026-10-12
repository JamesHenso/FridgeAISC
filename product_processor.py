import csv
import re
import unicodedata
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo

from retrieval import (
    DualFieldBM25Retriever,
    SearchDocument,
)


BASE_DIR = Path(__file__).resolve().parent

INGREDIENTS_FILE = BASE_DIR / "Ingredients_rows.csv"
NON_FOOD_FILE = BASE_DIR / "non_food_types.csv"
LOCAL_TIMEZONE = ZoneInfo("Asia/Ho_Chi_Minh")


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


SEARCH_DOCUMENTS = [
    SearchDocument(
        id=ingredient["id"],
        name=ingredient["name"],
        product_type="food",
        metadata={},
    )
    for ingredient in INGREDIENTS_BY_ID.values()
] + [
    SearchDocument(
        id=non_food["id"],
        name=non_food["name"],
        product_type="non_food",
        metadata={"category": non_food["category"]},
    )
    for non_food in NON_FOOD_BY_ID.values()
]


DUAL_BM25_RETRIEVER = DualFieldBM25Retriever(
    SEARCH_DOCUMENTS,
    accented_tokenizer=tokenize_with_accents,
    accentless_tokenizer=tokenize_without_accents,
    has_diacritics=has_diacritics,
)

# Dual BM25 is the only retriever initialized by the production API.
PRODUCT_RETRIEVER = DUAL_BM25_RETRIEVER


EXACT_DOCUMENTS = {}
for document in SEARCH_DOCUMENTS:
    key = normalize_for_accented_lookup(document.name)
    EXACT_DOCUMENTS.setdefault(key, []).append(document)


def current_local_date() -> str:
    return datetime.now(LOCAL_TIMEZONE).date().isoformat()


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
        "qty_display": item.get("quantity"),
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
