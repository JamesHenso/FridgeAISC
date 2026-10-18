# Product retrieval

The runtime no longer uses `FOOD_ALIASES` or `NON_FOOD_ALIASES`. Receipt lines
are checked against non-food signals/catalog entries first. Remaining lines are
searched against a food-only index, so kitchenware cannot win merely because a
token overlaps an ingredient name. Low-confidence rows retain the OCR name and
do not receive an ingredient ID. BM25 ranks candidates; it does not provide a
percentage similarity.

The following retrieval variants are available:

- `bm25`: plain BM25 baseline.
- `bm25-coverage`: BM25 with a document-coverage reranker. This is the runtime
  baseline for receipt descriptions with brand and packaging words.
- `dual-bm25`: accent-aware BM25 plus a lower-weight accentless OCR fallback,
  followed by coverage reranking. This is the runtime default.
- `char-ngram`: character 2-5 gram TF-IDF with cosine similarity.
- `fuzzy-wratio`: RapidFuzz WRatio over normalized names.
- `hybrid-rrf`: weighted Reciprocal Rank Fusion over dual BM25, char n-gram,
  and fuzzy. BM25 and n-gram receive more weight because WRatio creates many
  high-score ties for short product names.

## Evaluate algorithms

The former alias examples now live in `retrieval_test_cases.csv` as labelled
evaluation data. They do not participate in runtime matching.
`retrieval_ocr_test_cases.csv` contains noisy OCR variants and can be selected
with `--cases`.

```bash
python3 evaluate_retrieval.py --algorithm bm25
python3 evaluate_retrieval.py --algorithm bm25-coverage
python3 evaluate_retrieval.py --algorithm dual-bm25
python3 evaluate_retrieval.py --algorithm char-ngram
python3 evaluate_retrieval.py --algorithm fuzzy-wratio
python3 evaluate_retrieval.py --algorithm hybrid-rrf

# OCR-noise benchmark
python3 evaluate_retrieval.py \
  --algorithm hybrid-rrf \
  --cases retrieval_ocr_test_cases.csv
```

The report prints top-1 accuracy, top-k recall, and mean reciprocal rank (MRR).
Add real OCR examples to the CSV before tuning the algorithm.

## Run tests

```bash
python3 -m unittest discover -s tests -v
```

## Runtime behavior

Both receipt and product paths silently exclude confident non-food rows first,
then search the food-only catalog. Exact matches and confident retrieval
matches are assigned the canonical catalog name and `ingredient_id`
automatically. Fuzzy top matches at score >= 0.85 also auto-map for both scan
types. Below-threshold rows preserve the raw OCR name, leave `ingredient_id`
null, and expose food candidates for user selection. The old `process_products`
helper remains for offline retrieval evaluation.

Every returned item has this public shape:

```json
{
  "name": "Bơ lạt",
  "ingredient_id": "321bb46a-7bfe-40f2-8040-dcc400e3886f",
  "qty_display": "2",
  "unit_display": "hộp",
  "purchased_at": "2026-10-12",
  "expiry_date": "2026-10-12"
}
```

The example above documents the legacy helper. New API responses keep the
receipt unit and price, and never assign the current date as an expiry.
