"""FridgeWise OCR: separate receipt batches from single-product labels."""

import io
import os

from azure.ai.documentintelligence import DocumentIntelligenceClient
from azure.core.credentials import AzureKeyCredential
from dotenv import load_dotenv
from fastapi import FastAPI, File, HTTPException, UploadFile
from starlette.concurrency import run_in_threadpool

from ocr_parsers import product_label_fields, receipt_fields
from product_processor import classify_ocr_items


load_dotenv()
AZURE_ENDPOINT = os.getenv("AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT")
AZURE_KEY = os.getenv("AZURE_DOCUMENT_INTELLIGENCE_KEY")
if not AZURE_ENDPOINT or not AZURE_KEY:
    raise RuntimeError("Missing Azure Document Intelligence configuration in .env")

MAX_UPLOAD_BYTES = 4 * 1024 * 1024
SUPPORTED_SIGNATURES = (b"\xff\xd8\xff", b"\x89PNG\r\n\x1a\n", b"%PDF")


def upload_problem(image: bytes) -> str | None:
    """Azure returns a raw InvalidRequest blob for oversized or unsupported files; say it plainly."""
    if len(image) > MAX_UPLOAD_BYTES:
        return "Image is larger than the 4 MB Azure Document Intelligence limit."
    if not image.startswith(SUPPORTED_SIGNATURES):
        return "File content is not a JPG, PNG, or PDF image."
    return None

client = DocumentIntelligenceClient(
    endpoint=AZURE_ENDPOINT, credential=AzureKeyCredential(AZURE_KEY)
)
app = FastAPI(title="FridgeWise OCR API", version="0.2.0")


@app.get("/")
def root():
    return {"message": "FridgeWise OCR API is running"}


async def analyze_upload(file: UploadFile, model_id: str):
    if file.content_type not in {"image/jpeg", "image/png", "application/pdf"}:
        raise HTTPException(400, "Only JPG, PNG, and PDF files are supported.")
    image = await file.read()
    if not image:
        raise HTTPException(400, "Uploaded file is empty.")
    problem = upload_problem(image)
    if problem:
        raise HTTPException(400, problem)

    def analyze():
        return client.begin_analyze_document(model_id=model_id, body=io.BytesIO(image)).result()

    try:
        return await run_in_threadpool(analyze)
    except Exception as exc:
        raise HTTPException(502, f"Azure Document Intelligence error: {exc}") from exc


@app.post("/api/receipts/scan")
async def scan_receipt(file: UploadFile = File(...)):
    result = await analyze_upload(file, "prebuilt-receipt")
    parsed = receipt_fields(result)
    processed = classify_ocr_items(parsed["items"], parsed["purchased_at"])
    return {
        "mode": "receipt",
        "merchant": parsed["merchant"],
        "purchased_at": parsed["purchased_at"],
        "subtotal_vnd": parsed["subtotal_vnd"],
        "discount_vnd": parsed["discount_vnd"],
        "total_vnd": parsed["total_vnd"],
        "line_total_vnd": parsed["line_total_vnd"],
        "reconciliation_delta_vnd": parsed["reconciliation_delta_vnd"],
        **processed,
    }


@app.post("/api/products/scan")
async def scan_product(file: UploadFile = File(...)):
    result = await analyze_upload(file, "prebuilt-read")
    parsed = product_label_fields(result)
    if not parsed["raw_name"]:
        return {"mode": "product", "items": [], "excluded_items": [],
                "raw_lines": parsed["raw_lines"]}
    candidate = {**parsed, "source_type": "product"}
    processed = classify_ocr_items([candidate])
    return {"mode": "product", "manufactured_at": parsed["manufactured_at"],
            "raw_lines": parsed["raw_lines"], **processed}
