-- 004_new_tables.sql — profiles, fridges, fridge_members, foods, favorites + RLS + trigger
-- Chạy sau 003. Idempotent.
BEGIN;

-- profiles: Auth do Supabase SDK lo, đây chỉ là profile link auth.users (KHÔNG password)
CREATE TABLE IF NOT EXISTS profiles (
  id uuid PRIMARY KEY REFERENCES auth.users ON DELETE CASCADE,
  email text, name text, avatar_url text,
  created_at timestamptz DEFAULT now(), updated_at timestamptz DEFAULT now()
);

CREATE OR REPLACE FUNCTION handle_new_user() RETURNS trigger AS $$
BEGIN
  INSERT INTO public.profiles(id, email, name)
  VALUES (new.id, new.email, new.raw_user_meta_data->>'name')
  ON CONFLICT (id) DO NOTHING;
  RETURN new;
END $$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION handle_new_user();

-- fridges + share (Family = fridge_members + invite_code, không bảng Family)
CREATE TABLE IF NOT EXISTS fridges (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL REFERENCES auth.users,
  name text NOT NULL, invite_code text UNIQUE,
  created_at timestamptz DEFAULT now(), updated_at timestamptz DEFAULT now()
);
CREATE TABLE IF NOT EXISTS fridge_members (
  fridge_id uuid REFERENCES fridges ON DELETE CASCADE,
  user_id uuid REFERENCES auth.users ON DELETE CASCADE,
  role text DEFAULT 'member',
  PRIMARY KEY (fridge_id, user_id)
);

-- foods = FridgeItem: display (show) + base (tính), null base = chỉ biết có/không
CREATE TABLE IF NOT EXISTS foods (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  fridge_id uuid NOT NULL REFERENCES fridges ON DELETE CASCADE,
  ingredient_id uuid REFERENCES "Ingredients"(id) ON DELETE SET NULL,
  name text NOT NULL,
  qty_display numeric, unit_display text,
  qty_base numeric, base_unit text,
  expiry_date date, purchased_at timestamptz DEFAULT now(),
  created_at timestamptz DEFAULT now(), updated_at timestamptz DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_foods_fridge ON foods(fridge_id, expiry_date);

-- favorites = Like: tym reels = insert vào đây
CREATE TABLE IF NOT EXISTS favorites (
  user_id uuid REFERENCES auth.users ON DELETE CASCADE,
  recipe_id uuid REFERENCES "Recipe"(id) ON DELETE CASCADE,
  created_at timestamptz DEFAULT now(),
  PRIMARY KEY (user_id, recipe_id)
);
CREATE INDEX IF NOT EXISTS idx_fav_recipe ON favorites(recipe_id);

-- RLS (không đụng RLS bảng live để khỏi vỡ đọc hiện tại)
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE fridges ENABLE ROW LEVEL SECURITY;
ALTER TABLE fridge_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE foods ENABLE ROW LEVEL SECURITY;
ALTER TABLE favorites ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS own_profile ON profiles;
CREATE POLICY own_profile ON profiles FOR ALL USING (auth.uid() = id);
DROP POLICY IF EXISTS own_fridges ON fridges;
CREATE POLICY own_fridges ON fridges FOR ALL USING (auth.uid() = owner_id);
DROP POLICY IF EXISTS member_fridges_read ON fridges;
CREATE POLICY member_fridges_read ON fridges FOR SELECT USING (
  EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = fridges.id AND m.user_id = auth.uid()));
DROP POLICY IF EXISTS member_rw ON fridge_members;
CREATE POLICY member_rw ON fridge_members FOR SELECT USING (user_id = auth.uid());
DROP POLICY IF EXISTS owner_invite ON fridge_members;
CREATE POLICY owner_invite ON fridge_members FOR INSERT WITH CHECK (
  EXISTS (SELECT 1 FROM fridges f WHERE f.id = fridge_id AND f.owner_id = auth.uid()));
DROP POLICY IF EXISTS fridge_foods ON foods;
CREATE POLICY fridge_foods ON foods FOR ALL USING (
  EXISTS (SELECT 1 FROM fridges f WHERE f.id = foods.fridge_id AND f.owner_id = auth.uid())
  OR EXISTS (SELECT 1 FROM fridge_members m WHERE m.fridge_id = foods.fridge_id AND m.user_id = auth.uid()));
DROP POLICY IF EXISTS own_favorites ON favorites;
CREATE POLICY own_favorites ON favorites FOR ALL USING (auth.uid() = user_id);

COMMIT;
