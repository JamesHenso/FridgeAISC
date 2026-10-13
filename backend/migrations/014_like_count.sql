-- 014_like_count.sql — tym reels: tang/giam like_count atomic, san 0
-- Backend goi qua supabaseAdmin (service role) nen khong vuong RLS.
-- Chay sau 013 tren Supabase SQL editor. Idempotent.
BEGIN;

CREATE OR REPLACE FUNCTION bump_like_count(rid uuid, delta int)
RETURNS void AS $$
  UPDATE "Recipe" SET like_count = GREATEST(0, like_count + delta) WHERE id = rid;
$$ LANGUAGE sql SECURITY DEFINER SET search_path = public;

COMMIT;

-- Verify: 1 dong bump_like_count
SELECT proname FROM pg_proc WHERE proname = 'bump_like_count';
