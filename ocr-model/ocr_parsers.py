"""Field parsers shared by receipt and single-product OCR routes.

Keep extraction separate from catalogue retrieval so a catalogue match cannot
silently change a measured quantity, a date or a price.
"""

import re
from datetime import date


UNITS = r"kg|gram|gam|g|ml|lít|lit|l|bó|hộp|gói|chai|lon|tuýp|túi|lốc|cuộn|cái|quả|trái|vỉ|hũ|thùng"
QUANTITY_UNIT = re.compile(
    rf"(?<![\w.,])(?P<quantity>\d+(?:[.,]\d+)?)\s*(?P<unit>{UNITS})(?!\w)",
    re.IGNORECASE,
)
DATE_TOKEN = r"\d{1,2}[./-]\d{1,2}[./-]\d{2,4}"
MONEY_TOKEN = r"\d{1,3}(?:[.,]\d{3})+|\d{4,}"


def field_content(field):
    if field is None:
        return None
    content = getattr(field, "content", None)
    if content is not None:
        return str(content)
    for key in ("valueString", "valueNumber", "valueInteger", "valueDate", "content"):
        value = field.get(key) if hasattr(field, "get") else None
        if value is not None:
            return str(value)
    return None


def parse_quantity_unit(quantity, unit=None, row_text=None):
    """Read Azure's split or combined Quantity, then fall back to the row text."""
    raw = str(quantity).strip() if quantity is not None else ""
    matched = QUANTITY_UNIT.search(raw)
    if not matched and row_text:
        matched = QUANTITY_UNIT.search(str(row_text))
    if matched:
        number = float(matched.group("quantity").replace(",", "."))
        return number, matched.group("unit").lower()
    if re.fullmatch(r"\d+(?:[.,]\d+)?", raw):
        return float(raw.replace(",", ".")), unit.lower() if unit else None
    return None, unit.lower() if unit else None


def parse_money(value):
    if value is None:
        return None
    raw = str(value).strip().replace("đ", "").replace("₫", "").replace(" ", "")
    if re.fullmatch(r"\d{1,3}(?:[.,]\d{3})+", raw):
        return int(re.sub(r"[.,]", "", raw))
    if raw.isdecimal():
        return int(raw)
    return None


def parse_vietnamese_date(value):
    if value is None:
        return None
    raw = str(value).strip()
    if re.fullmatch(r"\d{4}-\d{2}-\d{2}", raw):
        try:
            return date.fromisoformat(raw).isoformat()
        except ValueError:
            return None
    match = re.search(DATE_TOKEN, raw)
    if not match:
        return None
    parts = re.split(r"[./-]", match.group())
    day, month, year = map(int, parts)
    if year < 100:
        year += 2000
    try:
        return date(year, month, day).isoformat()
    except ValueError:
        return None


def receipt_item_fields(item):
    fields = item.get("valueObject") or {}
    description = field_content(fields.get("Description"))
    if not description:
        return None
    row_text = field_content(item) or ""
    # A package size in the product name (e.g. "170g") is not the purchased
    # quantity. Search only after the Azure Description span as a fallback.
    quantity_text = row_text.replace(description, "", 1) if description in row_text else row_text
    quantity, unit = parse_quantity_unit(field_content(fields.get("Quantity")), row_text=quantity_text)
    description_field = fields.get("Description")
    return {
        "raw_name": description.strip(),
        "quantity": quantity,
        "unit": unit,
        "unit_price_vnd": parse_money(field_content(fields.get("Price"))),
        "price_vnd": parse_money(field_content(fields.get("TotalPrice"))),
        "ocr_confidence": getattr(description_field, "confidence", None),
        "raw_line": row_text,
    }


def receipt_fields(result):
    document = (result.documents or [None])[0]
    fields = (document.fields or {}) if document else {}
    item_field = fields.get("Items")
    values = item_field.get("valueArray") or [] if item_field else []
    items = [parsed for value in values if (parsed := receipt_item_fields(value))]
    line_total = sum(item["price_vnd"] for item in items if item["price_vnd"] is not None)
    subtotal = parse_money(field_content(fields.get("Subtotal")))
    return {
        "merchant": field_content(fields.get("MerchantName")),
        "purchased_at": parse_vietnamese_date(field_content(fields.get("TransactionDate"))),
        "subtotal_vnd": subtotal,
        "discount_vnd": (parse_money(field_content(fields.get("TotalDiscount") or fields.get("Discount")))
                         or _labelled_money(page_lines(result), r"giảm giá|chiết khấu|discount")),
        "total_vnd": parse_money(field_content(fields.get("Total"))),
        "line_total_vnd": line_total if items else None,
        "reconciliation_delta_vnd": line_total - subtotal if items and subtotal is not None else None,
        "items": items,
    }


def page_lines(result):
    return [line.content.strip() for page in (getattr(result, "pages", None) or [])
            for line in (page.lines or []) if getattr(line, "content", "").strip()]


def _labelled_date(lines, label):
    marker = re.compile(label, re.IGNORECASE)
    for index, line in enumerate(lines):
        if not marker.search(line):
            continue
        candidate = line
        # OCR may put the value on the following line of the same label block.
        if not re.search(DATE_TOKEN, candidate) and index + 1 < len(lines):
            candidate += " " + lines[index + 1]
        value = parse_vietnamese_date(candidate[marker.search(candidate).end():])
        if value:
            return value
    return None


def _labelled_money(lines, label):
    marker = re.compile(label, re.IGNORECASE)
    for index, line in enumerate(lines):
        hit = marker.search(line)
        if not hit:
            continue
        tail = line[hit.end():]
        if not re.search(MONEY_TOKEN, tail) and index + 1 < len(lines):
            tail += " " + lines[index + 1]
        amounts = re.findall(MONEY_TOKEN, tail)
        if amounts:
            return parse_money(amounts[0])
    return None


def product_label_fields(result):
    lines = page_lines(result)
    title_lines = []
    for line in lines:
        if re.search(r"\d{9,}|khối lượng|trọng lượng|mã truy", line, re.IGNORECASE):
            break
        if re.search(r"[A-Za-zÀ-ỹ]{2}", line) and not re.search(
            r"^(?:khối lượng|đơn giá|thành tiền|nsx|hsd|k\.?s\.?g|ktvs|mã|số|mix|\d)", line, re.IGNORECASE
        ) and not re.fullmatch(r"[\d\s|]+", line):
            title_lines.append(line)
    name = " ".join(title_lines[:2]) if title_lines else None
    weight = None
    for index, line in enumerate(lines):
        if re.search(r"khối lượng|trọng lượng|net weight", line, re.IGNORECASE):
            joined = " ".join(lines[index:index + 2])
            weight = QUANTITY_UNIT.search(joined)
            if weight:
                break
    quantity, unit = parse_quantity_unit(weight.group() if weight else None)
    return {
        "raw_name": name,
        "quantity": quantity,
        "unit": unit,
        "unit_price_vnd": _labelled_money(lines, r"đơn giá"),
        "price_vnd": _labelled_money(lines, r"thành tiền|tổng tiền"),
        "manufactured_at": _labelled_date(lines, r"\bNSX\b|ngày sản xuất"),
        "expiry_date": _labelled_date(lines, r"\bHSD\b|hạn sử dụng|sử dụng trước"),
        "raw_lines": lines,
    }
