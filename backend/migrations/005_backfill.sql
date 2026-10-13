-- 005_backfill.sql — làm sạch data crawl: trim quantity/unit, tách video_id, gắn is_estimate
-- Chạy sau 004. Chỉ update dòng bẩn (WHERE), chạy lại an toàn.
BEGIN;

-- quantity "3    " -> "3", "" -> NULL; unit trim tương tự
UPDATE "RecipeIngredient"
SET quantity = NULLIF(btrim(quantity), ''), unit = NULLIF(btrim(unit), '')
WHERE quantity IS DISTINCT FROM NULLIF(btrim(quantity), '')
   OR unit IS DISTINCT FROM NULLIF(btrim(unit), '');

-- không lượng = ước chừng, match kiểu có/không
UPDATE "RecipeIngredient" SET is_estimate = true
WHERE quantity IS NULL AND is_estimate IS DISTINCT FROM true;

-- video_id từ watch?v= / youtu.be/ / shorts/ (11 ký tự Youtube)
UPDATE "RecipeDetail" SET video_id =
  substring("videoUrl" from '(?:v=|youtu\.be/|shorts/)([A-Za-z0-9_-]{11})')
WHERE "videoUrl" IS NOT NULL AND video_id IS NULL;

-- đẩy video_id lên Recipe để feed reels query 1 bảng
UPDATE "Recipe" r SET video_id = d.video_id
FROM "RecipeDetail" d
WHERE d."recipeId" = r.id AND r.video_id IS NULL AND d.video_id IS NOT NULL;

COMMIT;

-- Verify: phải 0 bẩn còn lại
SELECT count(*) AS dirty_qty FROM "RecipeIngredient" WHERE quantity <> btrim(quantity) OR unit <> btrim(unit);
SELECT count(*) AS detail_no_videoid FROM "RecipeDetail" WHERE "videoUrl" IS NOT NULL AND video_id IS NULL;
SELECT count(*) AS recipes_with_video FROM "Recipe" WHERE video_id IS NOT NULL;
