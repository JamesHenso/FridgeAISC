import io
import os

from dotenv import load_dotenv
from fastapi import FastAPI, File, HTTPException, UploadFile
from starlette.concurrency import run_in_threadpool
from azure.core.credentials import AzureKeyCredential
from azure.ai.documentintelligence import DocumentIntelligenceClient
from product_processor import process_products

import re


KNOWN_UNITS = [
    "kg",
    "g",
    "gram",
    "gam",
    "ml",
    "l",
    "lit",
    "lít",
    "bó",
    "hộp",
    "gói",
    "chai",
    "lon",
    "tuýp",
    "túi",
    "lốc",
    "cuộn",
    "cái",
    "quả",
    "trái",
    "vỉ",
    "hũ",
    "thùng",
]


def extract_unit(item_content: str | None, quantity: str | None):
    if not item_content or quantity is None:
        return None

    quantity = str(quantity).strip()

    units_pattern = "|".join(
        sorted(map(re.escape, KNOWN_UNITS), key=len, reverse=True)
    )

    # Example:
    # "Thịt ba chỉ heo 0.452 kg 159,000 71,868"
    #                     ^^^^^^^^
    pattern = rf"""
        (?<![\d.,])
        {re.escape(quantity)}
        \s*
        (?P<unit>{units_pattern})
        \b
    """

    match = re.search(
        pattern,
        item_content,
        flags=re.IGNORECASE | re.VERBOSE,
    )

    if match:
        return match.group("unit").lower()

    return None

load_dotenv()

AZURE_ENDPOINT = os.getenv("AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT")
AZURE_KEY = os.getenv("AZURE_DOCUMENT_INTELLIGENCE_KEY")

if not AZURE_ENDPOINT or not AZURE_KEY:
    raise RuntimeError(
        "Missing AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT or "
        "AZURE_DOCUMENT_INTELLIGENCE_KEY in .env"
    )


client = DocumentIntelligenceClient(
    endpoint=AZURE_ENDPOINT,
    credential=AzureKeyCredential(AZURE_KEY),
)


def analyze_receipt_with_azure(file_bytes: bytes):
    """Run the synchronous Azure OCR operation outside the event loop."""
    poller = client.begin_analyze_document(
        model_id="prebuilt-receipt",
        body=io.BytesIO(file_bytes),
    )
    return poller.result()


app = FastAPI(
    title="FridgeWise Receipt OCR API",
    version="0.1.0",
)


@app.get("/")
def root():
    return {
        "message": "FridgeWise Receipt OCR API is running"
    }


def get_field_content(field):
    """
    Return the text/value Azure recognized for a receipt field.
    For the MVP we prefer the original recognized content because
    it is easy to inspect and send to the next normalization stage.
    """
    if field is None:
        return None

    if getattr(field, "content", None) is not None:
        return field.content

    value_string = field.get("valueString")
    if value_string is not None:
        return value_string

    value_number = field.get("valueNumber")
    if value_number is not None:
        return value_number

    value_integer = field.get("valueInteger")
    if value_integer is not None:
        return value_integer

    return None


@app.post("/api/receipts/scan")
async def scan_receipt(file: UploadFile = File(...)):
    allowed_types = {
        "image/jpeg",
        "image/png",
        "application/pdf",
    }

    if file.content_type not in allowed_types:
        raise HTTPException(
            status_code=400,
            detail="Only JPG, PNG, and PDF files are supported.",
        )

    try:
        file_bytes = await file.read()

        if not file_bytes:
            raise HTTPException(
                status_code=400,
                detail="Uploaded file is empty.",
            )

        result = await run_in_threadpool(
            analyze_receipt_with_azure,
            file_bytes,
        )

        if not result.documents:
            return {
                "items": [],
            }

        receipt = result.documents[0]
        fields = receipt.fields or {}

        items_field = fields.get("Items")

        extracted_items = []

        if items_field:
            items = items_field.get("valueArray") or []

            for item in items:
                item_object = item.get("valueObject") or {}

                description_field = item_object.get("Description")
                quantity_field = item_object.get("Quantity")
                price_field = item_object.get("Price")
                total_price_field = item_object.get("TotalPrice")

                description = get_field_content(description_field)
                quantity = get_field_content(quantity_field)

                item_content = getattr(item, "content", None)
                
                if item_content is None:
                    item_content = item.get("content")

                unit = extract_unit(
                    item_content=item_content,
                    quantity=quantity,
                )

                # FridgeWise only cares about actual detected products.
                if not description:
                    continue

                extracted_items.append(
                    {
                        "name": description,
                        "quantity": quantity,
                        "unit": unit,
                        "price": get_field_content(price_field),
                        "total_price": get_field_content(total_price_field),
                        "confidence": (
                            description_field.confidence
                            if description_field
                            else None
                        ),
                    }
                )
        processed = process_products(extracted_items)

        return {
            "items": processed["items"],
        }

    except HTTPException:
        raise

    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail=f"Azure Document Intelligence error: {str(exc)}",
        )
