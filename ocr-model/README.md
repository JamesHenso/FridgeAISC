# FridgeWise OCR

Two Azure Document Intelligence routes serve different review flows:

- `POST /api/receipts/scan` uses `prebuilt-receipt`. It returns receipt metadata,
  confirmed food and unresolved lines in `items`. Non-food lines are filtered
  internally and are not shown in the review screen. Receipt items have
  `expiry_date: null` until confirmed from a product label or entered manually.
- `POST /api/products/scan` uses `prebuilt-read`. It returns a single product
  candidate with labelled `HSD` (expiry), `NSX` (manufacture), weight, unit
  price and line amount when the label contains them. Ingredient matching uses
  the same thresholds as receipt rows.

Each item includes `raw_name`, canonical `name`, optional `ingredient_id`,
`qty_display`, `unit_display`, `unit_price_vnd`, `price_vnd`, `purchased_at`,
`expiry_date`, `match_status` and food catalogue `candidates`. A missing field
is `null`; OCR does not invent an expiry or a purchase date.

Classification checks the non-food catalog and household-item signals first.
Remaining lines are matched against a food-only index. Confident matches use
the canonical catalog name and `ingredient_id` automatically. Low-confidence
or ambiguous lines keep their OCR name and have no `ingredient_id`; candidate
food names are suggestions for the user's review.

## Run on Windows

```powershell
cd D:\FridgeAI\FridgeAISC\ocr-model
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn app:app --host 0.0.0.0 --port 8000
```

Configure `AZURE_DOCUMENT_INTELLIGENCE_ENDPOINT` and
`AZURE_DOCUMENT_INTELLIGENCE_KEY` in `.env`. Try the two routes at
`http://127.0.0.1:8000/docs`. Android uses `10.0.2.2:8000` to reach this
service on the host computer.

For the Android/backend persistence path, apply
`backend/migrations/016_ocr_product_receipt.sql` in Supabase SQL Editor before
creating new scan drafts.
