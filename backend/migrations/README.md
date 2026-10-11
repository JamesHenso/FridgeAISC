# Migrations — chạy theo số thứ tự trên Supabase SQL editor

```
001_easy_ids.sql    PK không con -> uuid (RecipeIngredient, RecipeDetail)
002_parent_ids.sql  PK cha -> uuid + giữ slug, relink FK (1 lần, có guard)
003_extend.sql      cột cộng thêm 4 bảng live (chạy lại an toàn)
004_new_tables.sql  profiles, fridges, fridge_members, foods, favorites + RLS
005_backfill.sql    trim quantity, tách video_id, is_estimate (chỉ sửa dòng bẩn)
006_scans.sql       scans + scan_items + bucket bills + RLS (nhập tay test, OCR sau)
007_grants.sql      bù GRANT Data API cho 5 bảng 004 + 2 bảng 006 (004 thiếu)
008_rls_hardening.sql USING+WITH CHECK tường minh, TO authenticated, member_leave DELETE
009_rls_recursion_fix.sql cắt vòng fridges<->members bằng hàm is_fridge_owner() definer
010_fridge_roster.sql  trigger auto-thêm owner + member thấy hết roster
```

Mỗi file là 1 transaction + có query verify ở cuối. Backup `_bak_*` tạo ở 001/002.
`qty_base` backfill bằng `toBase()` app-side (services layer) sau khi có code — không nhét map unit vào SQL.
