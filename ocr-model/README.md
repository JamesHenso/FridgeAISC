# FridgeAISC

Receipt OCR API using Azure Document Intelligence and Dual BM25 product
retrieval.

## Setup

Python 3.11 or newer is recommended.

```bash
python -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
cp .env.example .env
```

Fill in the two Azure Document Intelligence values in `.env`, then start the
API:

```bash
python -m uvicorn app:app --reload
```

Open `http://127.0.0.1:8000/docs` and call `POST /api/receipts/scan` with a
JPG, PNG, or PDF receipt.

The response contains only matched food items:

```json
{
  "items": [
    {
      "name": "Bơ lạt",
      "ingredient_id": "321bb46a-7bfe-40f2-8040-dcc400e3886f",
      "qty_display": "2",
      "unit_display": "hộp",
      "purchased_at": "2026-10-12",
      "expiry_date": "2026-10-12"
    }
  ]
}
```

## Tests

```bash
python -m unittest discover -s tests -v
```

See [RETRIEVAL.md](RETRIEVAL.md) for retrieval details and offline evaluation
commands.
