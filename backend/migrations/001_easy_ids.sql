-- 001_easy_ids.sql — PK không có bảng con: RecipeIngredient.id (text) + RecipeDetail.id (int) -> uuid
-- Chạy trên Supabase SQL editor. Idempotent (chạy lại an toàn), 1 transaction/file.
BEGIN;

-- Backup lần đầu (chạy lại thì giữ bản backup gốc)
CREATE TABLE IF NOT EXISTS _bak_recipeingredient AS SELECT * FROM "RecipeIngredient";
CREATE TABLE IF NOT EXISTS _bak_recipedetail AS SELECT * FROM "RecipeDetail";

-- ============ RecipeIngredient.id text -> uuid ============
ALTER TABLE "RecipeIngredient" ADD COLUMN IF NOT EXISTS new_id uuid;
UPDATE "RecipeIngredient" SET new_id = gen_random_uuid() WHERE new_id IS NULL;

DO $$ DECLARE c text; BEGIN
  SELECT conname INTO c FROM pg_constraint
  WHERE conrelid = '"RecipeIngredient"'::regclass AND contype = 'p';
  IF c IS NOT NULL THEN EXECUTE format('ALTER TABLE "RecipeIngredient" DROP CONSTRAINT %I', c); END IF;
END $$;

ALTER TABLE "RecipeIngredient" DROP COLUMN IF EXISTS id;
ALTER TABLE "RecipeIngredient" RENAME COLUMN new_id TO id;
ALTER TABLE "RecipeIngredient" ALTER COLUMN id SET NOT NULL;
ALTER TABLE "RecipeIngredient" ADD PRIMARY KEY (id);

-- ============ RecipeDetail.id int -> uuid ============
ALTER TABLE "RecipeDetail" ADD COLUMN IF NOT EXISTS new_id uuid;
UPDATE "RecipeDetail" SET new_id = gen_random_uuid() WHERE new_id IS NULL;

DO $$ DECLARE c text; BEGIN
  SELECT conname INTO c FROM pg_constraint
  WHERE conrelid = '"RecipeDetail"'::regclass AND contype = 'p';
  IF c IS NOT NULL THEN EXECUTE format('ALTER TABLE "RecipeDetail" DROP CONSTRAINT %I', c); END IF;
END $$;

ALTER TABLE "RecipeDetail" DROP COLUMN IF EXISTS id;
ALTER TABLE "RecipeDetail" RENAME COLUMN new_id TO id;
ALTER TABLE "RecipeDetail" ALTER COLUMN id SET NOT NULL;
ALTER TABLE "RecipeDetail" ADD PRIMARY KEY (id);

COMMIT;

-- Verify (phải ra 0 null + count khớp backup):
SELECT (SELECT count(*) FROM "RecipeIngredient") AS ri, (SELECT count(*) FROM _bak_recipeingredient) AS ri_bak,
       (SELECT count(*) FROM "RecipeDetail") AS rd, (SELECT count(*) FROM _bak_recipedetail) AS rd_bak;
SELECT count(*) AS ri_null_id FROM "RecipeIngredient" WHERE id IS NULL;
SELECT count(*) AS rd_null_id FROM "RecipeDetail" WHERE id IS NULL;
