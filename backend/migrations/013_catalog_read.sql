-- 013_catalog_read.sql — mở đọc catalog cho app/UI (Postman req 3, màn nhập tay chọn nguyên liệu)
-- Chạy sau 012 trên Supabase SQL editor. Idempotent.
BEGIN;

GRANT SELECT ON "Ingredients" TO authenticated;

ALTER TABLE "Ingredients" ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS catalog_read ON "Ingredients";
CREATE POLICY catalog_read ON "Ingredients" FOR SELECT TO authenticated USING (true);

COMMIT;

-- Verify: 1 dòng SELECT + 1 policy
SELECT table_name, privilege_type FROM information_schema.role_table_grants
WHERE grantee = 'authenticated' AND table_name = 'Ingredients';
SELECT policyname FROM pg_policies WHERE tablename = 'Ingredients';
