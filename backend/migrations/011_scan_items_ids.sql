-- 011_scan_items_ids.sql — nhập tay thay OCR: UI match ngầm từ Ingredients, user chỉ sửa qty/ngày
-- Chạy sau 010 trên Supabase SQL editor. Idempotent.
BEGIN;

ALTER TABLE scan_items
  ADD COLUMN IF NOT EXISTS ingredient_id uuid REFERENCES "Ingredients"(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS purchased_at date;
CREATE INDEX IF NOT EXISTS idx_scan_items_ingredient ON scan_items(ingredient_id);

COMMIT;

-- Verify: 2 dòng true
SELECT column_name FROM information_schema.columns
WHERE table_name = 'scan_items' AND column_name IN ('ingredient_id', 'purchased_at');
