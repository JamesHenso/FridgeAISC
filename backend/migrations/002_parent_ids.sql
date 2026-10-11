-- 002_parent_ids.sql — Recipe.id (slug) + Ingredients.id -> uuid, update FK bảng con
-- Chạy 1 lần sau 001. Giữ slug đọc được cho URL. Swap destructive có guard kiểu cột.
BEGIN;

CREATE TABLE IF NOT EXISTS _bak_recipe AS SELECT * FROM "Recipe";
CREATE TABLE IF NOT EXISTS _bak_ingredients AS SELECT * FROM "Ingredients";

-- 1. Slug + uuid mới ở bảng cha
ALTER TABLE "Recipe" ADD COLUMN IF NOT EXISTS slug text;
UPDATE "Recipe" SET slug = id WHERE slug IS NULL;
ALTER TABLE "Recipe" ADD COLUMN IF NOT EXISTS new_id uuid;
UPDATE "Recipe" SET new_id = gen_random_uuid() WHERE new_id IS NULL;

ALTER TABLE "Ingredients" ADD COLUMN IF NOT EXISTS new_id uuid;
UPDATE "Ingredients" SET new_id = gen_random_uuid() WHERE new_id IS NULL;

-- 2. Cột FK mới ở bảng con, backfill qua join với id cũ
ALTER TABLE "RecipeDetail" ADD COLUMN IF NOT EXISTS new_recipe_id uuid;
UPDATE "RecipeDetail" d SET new_recipe_id = r.new_id FROM "Recipe" r WHERE d."recipeId" = r.slug AND d.new_recipe_id IS NULL;

ALTER TABLE "RecipeIngredient" ADD COLUMN IF NOT EXISTS new_recipe_id uuid;
UPDATE "RecipeIngredient" ri SET new_recipe_id = r.new_id FROM "Recipe" r WHERE ri."recipeId" = r.slug AND ri.new_recipe_id IS NULL;

ALTER TABLE "RecipeIngredient" ADD COLUMN IF NOT EXISTS new_ingredient_id uuid;
UPDATE "RecipeIngredient" ri SET new_ingredient_id = i.new_id FROM "Ingredients" i WHERE ri."ingredientId" = i.id AND ri.new_ingredient_id IS NULL;

-- Mồ côi thì dừng (không nuốt lỗi join)
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM "RecipeDetail" WHERE new_recipe_id IS NULL) THEN RAISE EXCEPTION 'orphan RecipeDetail.recipeId'; END IF;
  IF EXISTS (SELECT 1 FROM "RecipeIngredient" WHERE new_recipe_id IS NULL) THEN RAISE EXCEPTION 'orphan RecipeIngredient.recipeId'; END IF;
END $$;
-- ingredientId cho phép null (nguyên liệu tự do), chỉ báo chứ không chặn:
-- SELECT * FROM "RecipeIngredient" WHERE "ingredientId" IS NOT NULL AND new_ingredient_id IS NULL;

-- 3. Drop mọi FK đang trỏ (tìm động, không cần biết tên constraint)
DO $$ DECLARE c record; BEGIN
  FOR c IN SELECT conname, conrelid::regclass::text AS t FROM pg_constraint
           WHERE contype = 'f' AND conrelid::regclass::text IN ('"RecipeDetail"', '"RecipeIngredient"')
  LOOP EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', c.t, c.conname); END LOOP;
END $$;

-- 4. Swap PK cha (chỉ chạy khi id còn là text — chạy lại an toàn)
DO $$ DECLARE c text; BEGIN
  IF (SELECT data_type FROM information_schema.columns WHERE table_name = 'Recipe' AND column_name = 'id') = 'text' THEN
    SELECT conname INTO c FROM pg_constraint WHERE conrelid = '"Recipe"'::regclass AND contype = 'p';
    IF c IS NOT NULL THEN EXECUTE format('ALTER TABLE "Recipe" DROP CONSTRAINT %I', c); END IF;
    ALTER TABLE "Recipe" DROP COLUMN id;
    ALTER TABLE "Recipe" RENAME COLUMN new_id TO id;
    ALTER TABLE "Recipe" ALTER COLUMN id SET NOT NULL;
    ALTER TABLE "Recipe" ADD PRIMARY KEY (id);
  END IF;
  IF (SELECT data_type FROM information_schema.columns WHERE table_name = 'Ingredients' AND column_name = 'id') = 'text' THEN
    SELECT conname INTO c FROM pg_constraint WHERE conrelid = '"Ingredients"'::regclass AND contype = 'p';
    IF c IS NOT NULL THEN EXECUTE format('ALTER TABLE "Ingredients" DROP CONSTRAINT %I', c); END IF;
    ALTER TABLE "Ingredients" DROP COLUMN id;
    ALTER TABLE "Ingredients" RENAME COLUMN new_id TO id;
    ALTER TABLE "Ingredients" ALTER COLUMN id SET NOT NULL;
    ALTER TABLE "Ingredients" ADD PRIMARY KEY (id);
  END IF;
END $$;

-- 5. Swap FK con + dựng lại FK
ALTER TABLE "RecipeDetail" DROP COLUMN IF EXISTS "recipeId";
ALTER TABLE "RecipeDetail" RENAME COLUMN new_recipe_id TO "recipeId";
ALTER TABLE "RecipeDetail" ALTER COLUMN "recipeId" SET NOT NULL;

ALTER TABLE "RecipeIngredient" DROP COLUMN IF EXISTS "recipeId";
ALTER TABLE "RecipeIngredient" RENAME COLUMN new_recipe_id TO "recipeId";
ALTER TABLE "RecipeIngredient" ALTER COLUMN "recipeId" SET NOT NULL;
ALTER TABLE "RecipeIngredient" DROP COLUMN IF EXISTS "ingredientId";
ALTER TABLE "RecipeIngredient" RENAME COLUMN new_ingredient_id TO "ingredientId";
ALTER TABLE "RecipeIngredient" ALTER COLUMN "ingredientId" DROP NOT NULL;

ALTER TABLE "RecipeDetail" ADD CONSTRAINT fk_recipedetail_recipe
  FOREIGN KEY ("recipeId") REFERENCES "Recipe"(id) ON DELETE CASCADE;
ALTER TABLE "RecipeIngredient" ADD CONSTRAINT fk_ri_recipe
  FOREIGN KEY ("recipeId") REFERENCES "Recipe"(id) ON DELETE CASCADE;
ALTER TABLE "RecipeIngredient" ADD CONSTRAINT fk_ri_ingredient
  FOREIGN KEY ("ingredientId") REFERENCES "Ingredients"(id) ON DELETE SET NULL;

COMMIT;

-- Verify: 3 dòng đều phải 0
SELECT count(*) AS orphan_detail FROM "RecipeDetail" d LEFT JOIN "Recipe" r ON r.id = d."recipeId" WHERE r.id IS NULL;
SELECT count(*) AS orphan_ri_recipe FROM "RecipeIngredient" ri LEFT JOIN "Recipe" r ON r.id = ri."recipeId" WHERE r.id IS NULL;
SELECT count(*) AS orphan_ri_ing FROM "RecipeIngredient" ri LEFT JOIN "Ingredients" i ON i.id = ri."ingredientId" WHERE ri."ingredientId" IS NOT NULL AND i.id IS NULL;
