-- 007_grants.sql — bù GRANT Data API cho 5 bảng ở 004 (004 không có GRANT)
-- Chạy trên Supabase SQL editor sau 006. Idempotent.
BEGIN;

-- Không GRANT thì role anon/authenticated không qua được Data API dù RLS đã mở
GRANT ALL ON profiles, fridges, fridge_members, foods, favorites TO authenticated, service_role;
GRANT ALL ON scans, scan_items TO authenticated, service_role;

COMMIT;

-- Verify: 7 dòng, đều có privilege cho authenticated
SELECT table_name, privilege_type FROM information_schema.role_table_grants
WHERE grantee = 'authenticated' AND table_name IN
  ('profiles', 'fridges', 'fridge_members', 'foods', 'favorites', 'scans', 'scan_items')
ORDER BY 1, 2;
