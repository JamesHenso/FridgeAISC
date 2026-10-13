# FridgeWise — Schema Database (khớp Supabase live)

> Verified bằng REST trên data thật (Oct 2026). Bảng live là PascalCase số ít.
> Migration trong `backend/migrations/`, chạy theo số thứ tự trên SQL editor.

## 1. Live hiện tại (đã có data crawl monngonmoingay)

```
Recipe(id text slug, name, description, imageUrl, category, method)
RecipeDetail(id int, recipeId text -> Recipe.id, servings int, time int phút,
  difficulty, videoUrl youtube, preparation, instructions, servingText)
RecipeIngredient(id text RI..., recipeId text, ingredientId text,
  name denormalized, quantity text nullable bẩn ("3    ", null), unit nullable)
Ingredients(id text ING..., name)
```

Quirk data: `quantity` text cần trim/parse, null = chỉ check có/không.
`preparation` dùng shorthand `1M/1m/2M` (muỗng canh/cf). `description` lẫn HTML entity.
`RecipeDetail` tách sẵn + có data → **giữ tách, bỏ quyết định gộp cũ**.

## 2. Target (sau migrations/)

```
Recipe(+slug giữ id cũ, +video_id, +video_provider, +like_count)
RecipeDetail(+video_id; id int -> uuid)
RecipeIngredient(+qty_base numeric, +base_unit, +is_estimate; id text -> uuid)
Ingredients(+base_unit, +pcs_to_gram, +category, +image_url; id text -> uuid)
profiles / fridges / fridge_members / foods / favorites (mới, uuid từ đầu)
```

* Tồn kho `foods`: 2 lớp `qty_display/unit_display` (show) + `qty_base/base_unit` (tính).
* Reels = `Recipe WHERE video_id NOT NULL` (chỉ Shorts), không infra upload.
* Tym = `INSERT favorites + like_count+1` cùng transaction. `Family` = `fridge_members + invite_code`.
* Auth: Supabase SDK, `profiles.id -> auth.users`, trigger `handle_new_user()`, RLS `auth.uid()=id`.

## 3. Thứ tự migration

```
001_easy_ids.sql    RecipeIngredient.id, RecipeDetail.id -> uuid (không con, làm trước)
002_parent_ids.sql  Recipe.id, Ingredients.id -> uuid + giữ slug, update FK bảng con
003_extend.sql      ADD COLUMN cộng thêm lên 4 bảng live (toàn IF NOT EXISTS)
004_new_tables.sql  profiles, fridges, fridge_members, foods, favorites + RLS + trigger
005_backfill.sql    trim quantity/unit, tách video_id, gắn is_estimate
```

`qty_base` backfill chạy app-side bằng `toBase()` (services layer) sau khi có code, không nhét map unit vào SQL.
