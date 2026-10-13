-- 010_fridge_roster.sql — danh sách family đầy đủ
-- 2 thiếu sau khi test live 009:
--   1. Chủ tủ không có dòng trong fridge_members -> danh sách thiếu chủ.
--      Trigger tự thêm owner (role='owner') khi tạo tủ + backfill tủ cũ.
--   2. Member chỉ thấy dòng của mình -> không thấy bạn cùng tủ.
--      Nới member_list: member hoặc owner của tủ thì thấy hết roster.
-- Chạy trên Supabase SQL editor sau 009. Idempotent.
BEGIN;

CREATE OR REPLACE FUNCTION is_fridge_member(fid uuid)
RETURNS boolean AS $$
  SELECT EXISTS (
    SELECT 1 FROM fridge_members WHERE fridge_id = fid AND user_id = (select auth.uid()));
$$ LANGUAGE sql SECURITY DEFINER SET search_path = public;

DROP POLICY IF EXISTS member_list ON fridge_members;
CREATE POLICY member_list ON fridge_members FOR SELECT TO authenticated USING (
  is_fridge_member(fridge_id) OR is_fridge_owner(fridge_id));

CREATE OR REPLACE FUNCTION add_owner_as_member()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.fridge_members(fridge_id, user_id, role)
  VALUES (new.id, new.owner_id, 'owner')
  ON CONFLICT (fridge_id, user_id) DO NOTHING;
  RETURN new;
END $$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP TRIGGER IF EXISTS on_fridge_created ON fridges;
CREATE TRIGGER on_fridge_created AFTER INSERT ON fridges
  FOR EACH ROW EXECUTE FUNCTION add_owner_as_member();

-- Backfill tủ đã tồn tại trước trigger
INSERT INTO fridge_members(fridge_id, user_id, role)
SELECT f.id, f.owner_id, 'owner' FROM fridges f
LEFT JOIN fridge_members m ON m.fridge_id = f.id AND m.user_id = f.owner_id
WHERE m.fridge_id IS NULL
ON CONFLICT (fridge_id, user_id) DO NOTHING;

COMMIT;

-- Verify: trigger tồn tại + còn đủ 3 policies
SELECT trigger_name FROM information_schema.triggers WHERE trigger_name = 'on_fridge_created';
SELECT policyname FROM pg_policies WHERE tablename = 'fridge_members' ORDER BY 1;
