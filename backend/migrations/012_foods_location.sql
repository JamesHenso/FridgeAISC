-- 012_foods_location.sql — drag-drop xếp ngăn đông/mát rồi mới commit vào inventory
-- Chạy sau 011 trên Supabase SQL editor. Idempotent.
BEGIN;

ALTER TABLE foods ADD COLUMN IF NOT EXISTS location text NOT NULL DEFAULT 'fridge';
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'foods_location_check') THEN
    ALTER TABLE foods ADD CONSTRAINT foods_location_check CHECK (location IN ('fridge', 'freezer'));
  END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_foods_fridge_location ON foods(fridge_id, location);

COMMIT;

-- Verify: 1 dòng true + 1 dòng check
SELECT column_name FROM information_schema.columns
WHERE table_name = 'foods' AND column_name = 'location';
SELECT conname FROM pg_constraint WHERE conname = 'foods_location_check';
