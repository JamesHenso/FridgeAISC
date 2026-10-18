-- Preserve OCR provenance and paid amounts on scan drafts and inventory lots.
-- Run after 015. Additive; existing rows remain valid.
BEGIN;

ALTER TABLE scans ADD COLUMN IF NOT EXISTS source_type text;
ALTER TABLE scans ADD COLUMN IF NOT EXISTS merchant text;
ALTER TABLE scans ADD COLUMN IF NOT EXISTS receipt_total_vnd numeric;
ALTER TABLE scans ADD COLUMN IF NOT EXISTS receipt_subtotal_vnd numeric;
ALTER TABLE scans ADD COLUMN IF NOT EXISTS receipt_discount_vnd numeric;
ALTER TABLE scans ADD COLUMN IF NOT EXISTS receipt_line_total_vnd numeric;

ALTER TABLE scan_items ADD COLUMN IF NOT EXISTS raw_name text;
ALTER TABLE scan_items ADD COLUMN IF NOT EXISTS unit_price_vnd numeric;
ALTER TABLE scan_items ADD COLUMN IF NOT EXISTS price_vnd numeric;
ALTER TABLE scan_items ADD COLUMN IF NOT EXISTS source_type text;
ALTER TABLE scan_items ADD COLUMN IF NOT EXISTS match_status text;

ALTER TABLE foods ADD COLUMN IF NOT EXISTS raw_name text;
ALTER TABLE foods ADD COLUMN IF NOT EXISTS unit_price_vnd numeric;
ALTER TABLE foods ADD COLUMN IF NOT EXISTS price_vnd numeric;
ALTER TABLE foods ADD COLUMN IF NOT EXISTS source_type text;

COMMIT;

-- Expect 6 + 5 + 4 = 15 rows.
SELECT table_name, column_name FROM information_schema.columns
WHERE table_schema = 'public'
  AND ((table_name = 'scans' AND column_name IN ('source_type', 'merchant', 'receipt_total_vnd',
                                                 'receipt_subtotal_vnd', 'receipt_discount_vnd', 'receipt_line_total_vnd'))
    OR (table_name = 'scan_items' AND column_name IN ('raw_name', 'unit_price_vnd', 'price_vnd', 'source_type', 'match_status'))
    OR (table_name = 'foods' AND column_name IN ('raw_name', 'unit_price_vnd', 'price_vnd', 'source_type')))
ORDER BY table_name, column_name;
