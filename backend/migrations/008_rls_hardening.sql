-- 008_rls_hardening.sql — vá RLS 004+006 theo checklist Supabase
-- Chạy trên Supabase SQL editor sau 007. Idempotent (DROP IF EXISTS + CREATE).
-- Vá 3 lỗ:
--   1. Mọi policy FOR ALL thiếu WITH CHECK -> user sửa owner_id/fridge_id/user_id
--      để chiếm tủ / chuyển đồ sang tủ người khác. Giờ USING + WITH CHECK tường minh.
--   2. fridge_members không có DELETE -> không ai rời/xóa member được. Thêm member_leave.
--   3. Chủ tủ không xem được danh sách member (member_rw chỉ cho xem dòng của mình).
--      Nới SELECT cho owner.
-- Không dùng auth.role() (deprecated); dùng TO authenticated + (select auth.uid()).
BEGIN;

-- ============ profiles ============
DROP POLICY IF EXISTS own_profile ON profiles;
CREATE POLICY own_profile ON profiles FOR ALL TO authenticated
USING ((select auth.uid()) = id)
WITH CHECK ((select auth.uid()) = id);

-- ============ fridges ============
DROP POLICY IF EXISTS own_fridges ON fridges;
CREATE POLICY own_fridges ON fridges FOR ALL TO authenticated
USING ((select auth.uid()) = owner_id)
WITH CHECK ((select auth.uid()) = owner_id);

DROP POLICY IF EXISTS member_fridges_read ON fridges;
CREATE POLICY member_fridges_read ON fridges FOR SELECT TO authenticated USING (
  EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = fridges.id AND m.user_id = (select auth.uid())));

-- ============ fridge_members ============
DROP POLICY IF EXISTS member_rw ON fridge_members;
CREATE POLICY member_list ON fridge_members FOR SELECT TO authenticated USING (
  user_id = (select auth.uid())
  OR EXISTS (SELECT 1 FROM fridges f WHERE f.id = fridge_members.fridge_id AND f.owner_id = (select auth.uid())));

DROP POLICY IF EXISTS owner_invite ON fridge_members;
CREATE POLICY owner_invite ON fridge_members FOR INSERT TO authenticated WITH CHECK (
  EXISTS (SELECT 1 FROM fridges f WHERE f.id = fridge_id AND f.owner_id = (select auth.uid())));

CREATE POLICY member_leave ON fridge_members FOR DELETE TO authenticated USING (
  user_id = (select auth.uid())
  OR EXISTS (SELECT 1 FROM fridges f WHERE f.id = fridge_members.fridge_id AND f.owner_id = (select auth.uid())));

-- ============ foods ============
DROP POLICY IF EXISTS fridge_foods ON foods;
CREATE POLICY fridge_foods ON foods FOR ALL TO authenticated
USING (
  EXISTS (SELECT 1 FROM fridges f WHERE f.id = foods.fridge_id AND f.owner_id = (select auth.uid()))
  OR EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = foods.fridge_id AND m.user_id = (select auth.uid())))
WITH CHECK (
  EXISTS (SELECT 1 FROM fridges f WHERE f.id = foods.fridge_id AND f.owner_id = (select auth.uid()))
  OR EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = foods.fridge_id AND m.user_id = (select auth.uid())));

-- ============ favorites ============
DROP POLICY IF EXISTS own_favorites ON favorites;
CREATE POLICY own_favorites ON favorites FOR ALL TO authenticated
USING ((select auth.uid()) = user_id)
WITH CHECK ((select auth.uid()) = user_id);

-- ============ scans ============
DROP POLICY IF EXISTS fridge_scans ON scans;
CREATE POLICY fridge_scans ON scans FOR ALL TO authenticated
USING (
  EXISTS (SELECT 1 FROM fridges f WHERE f.id = scans.fridge_id AND f.owner_id = (select auth.uid()))
  OR EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = scans.fridge_id AND m.user_id = (select auth.uid())))
WITH CHECK (
  EXISTS (SELECT 1 FROM fridges f WHERE f.id = scans.fridge_id AND f.owner_id = (select auth.uid()))
  OR EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = scans.fridge_id AND m.user_id = (select auth.uid())));

-- ============ scan_items ============
DROP POLICY IF EXISTS fridge_scan_items ON scan_items;
CREATE POLICY fridge_scan_items ON scan_items FOR ALL TO authenticated
USING (
  EXISTS (SELECT 1 FROM scans s
    WHERE s.id = scan_items.scan_id AND (
      EXISTS (SELECT 1 FROM fridges f WHERE f.id = s.fridge_id AND f.owner_id = (select auth.uid()))
      OR EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = s.fridge_id AND m.user_id = (select auth.uid())))))
WITH CHECK (
  EXISTS (SELECT 1 FROM scans s
    WHERE s.id = scan_items.scan_id AND (
      EXISTS (SELECT 1 FROM fridges f WHERE f.id = s.fridge_id AND f.owner_id = (select auth.uid()))
      OR EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = s.fridge_id AND m.user_id = (select auth.uid())))));

COMMIT;

-- Verify: đủ 8 policies, đúng tên
SELECT tablename, policyname, roles, cmd FROM pg_policies
WHERE tablename IN ('profiles', 'fridges', 'fridge_members', 'foods', 'favorites', 'scans', 'scan_items')
ORDER BY 1, 2;
