-- 009_rls_recursion_fix.sql — cắt vòng lặp fridges <-> fridge_members
-- 008 tạo vòng: fridges.member_fridges_read -> fridge_members -> fridges (qua check owner).
-- Vá: check owner gom vào 1 hàm SECURITY DEFINER (chạy quyền postgres, bypass RLS
-- nên không đệ quy), policy chỉ gọi hàm. Hàm chỉ soi đúng user đang gọi (auth.uid()),
-- không lộ data người khác.
-- Chạy trên Supabase SQL editor sau 008. Idempotent.
BEGIN;

CREATE OR REPLACE FUNCTION is_fridge_owner(fid uuid)
RETURNS boolean AS $$
  SELECT EXISTS (SELECT 1 FROM fridges WHERE id = fid AND owner_id = (select auth.uid()));
$$ LANGUAGE sql SECURITY DEFINER SET search_path = public;

DROP POLICY IF EXISTS member_list ON fridge_members;
CREATE POLICY member_list ON fridge_members FOR SELECT TO authenticated USING (
  user_id = (select auth.uid()) OR is_fridge_owner(fridge_id));

DROP POLICY IF EXISTS owner_invite ON fridge_members;
CREATE POLICY owner_invite ON fridge_members FOR INSERT TO authenticated
WITH CHECK (is_fridge_owner(fridge_id));

DROP POLICY IF EXISTS member_leave ON fridge_members;
CREATE POLICY member_leave ON fridge_members FOR DELETE TO authenticated USING (
  user_id = (select auth.uid()) OR is_fridge_owner(fridge_id));

COMMIT;

-- Verify
SELECT policyname FROM pg_policies WHERE tablename = 'fridge_members' ORDER BY 1;
