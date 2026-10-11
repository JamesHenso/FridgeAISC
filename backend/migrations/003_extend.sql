-- 003_extend.sql — cột cộng thêm (toàn ADD IF NOT EXISTS, chạy lại an toàn, chạy sau 002)
BEGIN;

ALTER TABLE "Recipe" ADD COLUMN IF NOT EXISTS video_id text;
ALTER TABLE "Recipe" ADD COLUMN IF NOT EXISTS video_provider text DEFAULT 'youtube';
ALTER TABLE "Recipe" ADD COLUMN IF NOT EXISTS like_count int NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_recipe_video ON "Recipe"(video_id) WHERE video_id IS NOT NULL;

ALTER TABLE "RecipeDetail" ADD COLUMN IF NOT EXISTS video_id text;

ALTER TABLE "RecipeIngredient" ADD COLUMN IF NOT EXISTS qty_base numeric;
ALTER TABLE "RecipeIngredient" ADD COLUMN IF NOT EXISTS base_unit text;
ALTER TABLE "RecipeIngredient" ADD COLUMN IF NOT EXISTS is_estimate bool DEFAULT false;
CREATE INDEX IF NOT EXISTS idx_ri_recipe_ing ON "RecipeIngredient"("recipeId", "ingredientId");

ALTER TABLE "Ingredients" ADD COLUMN IF NOT EXISTS base_unit text NOT NULL DEFAULT 'pcs';
ALTER TABLE "Ingredients" ADD COLUMN IF NOT EXISTS pcs_to_gram int;
ALTER TABLE "Ingredients" ADD COLUMN IF NOT EXISTS category text;
ALTER TABLE "Ingredients" ADD COLUMN IF NOT EXISTS image_url text;
CREATE INDEX IF NOT EXISTS idx_ingredients_name ON "Ingredients"(name);

COMMIT;
