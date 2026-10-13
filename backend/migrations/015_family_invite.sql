-- 015_family_invite.sql — Family join-by-code: tu nao thieu code thi backfill, insert sau tu sinh
-- Tao tu qua REST truc tiep van co code. Chay sau 014 tren Supabase SQL editor. Idempotent.
BEGIN;

CREATE OR REPLACE FUNCTION gen_invite_code() RETURNS text AS $$
  SELECT upper(substring(md5(gen_random_uuid()::text) from 1 for 6));
$$ LANGUAGE sql;

CREATE OR REPLACE FUNCTION set_invite_code() RETURNS trigger AS $$
BEGIN
  IF NEW.invite_code IS NULL THEN
    LOOP
      NEW.invite_code := gen_invite_code();
      EXIT WHEN NOT EXISTS (SELECT 1 FROM fridges WHERE invite_code = NEW.invite_code);
    END LOOP;
  END IF;
  RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_fridge_invite ON fridges;
CREATE TRIGGER trg_fridge_invite BEFORE INSERT ON fridges
  FOR EACH ROW EXECUTE FUNCTION set_invite_code();

-- Backfill: deterministic theo id, thuc te unique (md5 input unique)
UPDATE fridges SET invite_code = upper(substring(md5('invite-' || id::text) from 1 for 6))
WHERE invite_code IS NULL;

COMMIT;

-- Verify: 0 dong thieu code + trigger ton tai
SELECT count(*) FROM fridges WHERE invite_code IS NULL;
SELECT trigger_name FROM information_schema.triggers WHERE trigger_name = 'trg_fridge_invite';
