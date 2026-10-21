# Ingredient categories and icons

`Ingredients.image_url` already exists and is used for catalog thumbnails. Group icons use portable, URL-encoded `/assets/<file>.webp` paths. No extra column or schema migration is required. Absolute URLs remain supported for custom ingredient thumbnails.

The Express backend serves `backend/assets` at `/assets` in both dev and compiled builds. Deploy/copy the `assets` folder alongside `dist`. Android resolves relative paths against `api.baseUrl`, not the OCR service address.

Inventory returns `image_url`; recipe detail includes `ingredients[].image_url` and `category`; scan creation includes `items[].image_url`. Ingredient IDs, names, quantities and base units are unchanged. Missing icons use the UI fallback; the map does not substitute unrelated images for milk, oils, flour or prepared foods.

## Maintain the mapping

Edit `src/lib/ingredient-appearance.ts`. Rules prioritize product identity before flavor and distinguish whole words (e.g. fish fillet versus pear). `gia_cam.webp` is an egg illustration, and `bo.webp` is butter/margarine. Pork cuts share the pork category but use different icons.

From `backend`:

```bash
pnpm tsx scripts/gen-ingredient-category.mjs --dry
# Review reports/ingredient-appearance-plan.json before applying.
pnpm tsx scripts/gen-ingredient-category.mjs --apply
pnpm tsx scripts/gen-ingredient-category.mjs --dry
```

Dry run is the default. Apply saves a timestamped `backups/ingredients-*.json` before writing, preserves custom absolute image URLs, and checks old category/image values to avoid overwriting concurrent edits. Batches are not one database transaction; if interrupted, inspect the backup and re-run dry mode before continuing. Re-running with unchanged rules produces zero changes.

Backups/reports are local and gitignored. Each backup contains `id`, `name`, `category`, `image_url`; restore only the last two fields by ID when needed.

## Current catalog mapping

| Category | Rows | Asset paths |
| --- | ---: | --- |
| Bánh & thực phẩm chế biến | 64 | — |
| Bánh tráng | 8 | `/assets/banh_trang.webp` |
| Bún, mì & phở | 168 | `/assets/bun.webp` |
| Bơ & chất béo | 3 | `/assets/bo.webp` |
| Cá & hải sản | 122 | `/assets/hai_san.webp` |
| Cần rà soát | 33 | — |
| Củ & gia vị tươi | 47 | `/assets/cu.webp` |
| Dầu & mỡ | 25 | — |
| Gia vị & nước chấm | 186 | `/assets/hat_nem.webp` |
| Khác | 9 | — |
| Mắm | 4 | `/assets/mam_tom.webp` |
| Ngũ cốc & bột | 43 | — |
| Nấm | 21 | `/assets/nam.webp` |
| Rau & quả dùng nấu ăn | 254 | `/assets/rau.webp` |
| Sữa & phô mai | 81 | — |
| Thịt bò & bê | 43 | `/assets/thit_bo.webp` |
| Thịt chế biến | 75 | `/assets/cha_bong_gion.webp` |
| Thịt gia cầm | 38 | `/assets/thit_ga.webp` |
| Thịt heo | 37 | `/assets/ba_chi.webp`, `/assets/bap%2C%20chan_gio.webp`, `/assets/suon.webp`, `/assets/thit-heo.webp`, `/assets/thit_dui.webp`, `/assets/xuong.webp` |
| Thịt khác | 1 | — |
| Trái cây | 77 | `/assets/trai_cay.webp` |
| Trứng | 13 | `/assets/gia_cam.webp` |
| Đường & chất tạo ngọt | 9 | `/assets/duong.webp` |
| Đậu & hạt | 21 | — |
| Đậu phụ & thực phẩm chay | 16 | — |
| Đồ uống | 11 | — |

`Cần rà soát` contains scraped meal instructions or mixed entries, not reliable ingredient identities; these are retained for review, never deleted. `Khác` contains names without a confident rule.
