-- 006_scans.sql — bill quét OCR: scans (1 bill) + scan_items (dòng draft) + bucket bills
-- Chạy sau 005 trên Supabase SQL editor. Idempotent (toàn IF NOT EXISTS / DROP IF EXISTS).
-- Nhập tay test: insert scan_items trực tiếp qua PostgREST, commit qua Express POST /scans/:id/commit.
BEGIN;

CREATE TABLE IF NOT EXISTS scans (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  fridge_id uuid NOT NULL REFERENCES fridges ON DELETE CASCADE,
  bill_url text,
  status text NOT NULL DEFAULT 'draft',
  created_by uuid REFERENCES auth.users ON DELETE SET NULL,
  created_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS scan_items (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  scan_id uuid NOT NULL REFERENCES scans ON DELETE CASCADE,
  name text NOT NULL,
  qty_display numeric, unit_display text,
  qty_base numeric, base_unit text,
  expiry_date date, location text DEFAULT 'fridge',
  created_at timestamptz DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_scan_items_scan ON scan_items(scan_id);
CREATE INDEX IF NOT EXISTS idx_scans_fridge ON scans(fridge_id);

-- Bucket ảnh bill (private; Express upload bằng service_role nên không cần storage policy cho MVP)
INSERT INTO storage.buckets (id, name, public)
VALUES ('bills', 'bills', false)
ON CONFLICT (id) DO NOTHING;

-- Data API: bảng mới cần GRANT tường minh thì anon/authenticated mới thấy được
GRANT ALL ON scans, scan_items TO authenticated, service_role;

-- RLS (copy mẫu policy foods ở 004: member của tủ mới đọc/ghi)
ALTER TABLE scans ENABLE ROW LEVEL SECURITY;
ALTER TABLE scan_items ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS fridge_scans ON scans;
CREATE POLICY fridge_scans ON scans FOR ALL TO authenticated USING (
  EXISTS (SELECT 1 FROM fridges f WHERE f.id = scans.fridge_id AND f.owner_id = auth.uid())
  OR EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = scans.fridge_id AND m.user_id = auth.uid()))
WITH CHECK (
  EXISTS (SELECT 1 FROM fridges f WHERE f.id = scans.fridge_id AND f.owner_id = auth.uid())
  OR EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = scans.fridge_id AND m.user_id = auth.uid()));

DROP POLICY IF EXISTS fridge_scan_items ON scan_items;
CREATE POLICY fridge_scan_items ON scan_items FOR ALL TO authenticated USING (
  EXISTS (SELECT 1 FROM scans s
    WHERE s.id = scan_items.scan_id AND (
      EXISTS (SELECT 1 FROM fridges f WHERE f.id = s.fridge_id AND f.owner_id = auth.uid())
      OR EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = s.fridge_id AND m.user_id = auth.uid()))))
WITH CHECK (
  EXISTS (SELECT 1 FROM scans s
    WHERE s.id = scan_items.scan_id AND (
      EXISTS (SELECT 1 FROM fridges f WHERE f.id = s.fridge_id AND f.owner_id = auth.uid())
      OR EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = s.fridge_id AND m.user_id = auth.uid()))));

COMMIT;

-- Verify: 2 dòng true, policies >= 2
SELECT tablename, rowsecurity FROM pg_tables WHERE tablename IN ('scans', 'scan_items');
SELECT policyname FROM pg_policies WHERE tablename IN ('scans', 'scan_items');
SELECT count(*) AS bills_bucket FROM storage.buckets WHERE id = 'bills';
