import { supabaseAdmin } from "../config/supabase.js";
import { fetchRecipeIngredients } from "../repositories/recipes.repo.js";
import { containingAll } from "../lib/matchAll.js";

export interface FridgeItem {
  ingredientId?: string | null;
  name?: string;
  qty_base?: number | null;
}

export interface RankedRecipe {
  recipeId: string;
  missing: number;
  enough: boolean;
}

// Pure: rank recipes by missing count; qty_base compare when both sides known.
export function rankByMissing(
  recipeRows: Array<{ recipeId: string; ingredientId: string | null; qty_base: number | null }>,
  fridge: FridgeItem[],
  requireAll = false,
): RankedRecipe[] {
  const haveIds = new Set(fridge.map((f) => f.ingredientId).filter(Boolean) as string[]);
  const fridgeQty = new Map(
    fridge.filter((f) => f.ingredientId).map((f) => [f.ingredientId as string, f.qty_base ?? null]),
  );
  const byRecipe = new Map<string, typeof recipeRows>();
  for (const r of recipeRows) {
    const l = byRecipe.get(r.recipeId) ?? [];
    l.push(r);
    byRecipe.set(r.recipeId, l);
  }
  const out: RankedRecipe[] = [];
  for (const [id, rows] of byRecipe) {
    let missing = 0;
    for (const row of rows) {
      if (!row.ingredientId || !haveIds.has(row.ingredientId)) {
        if (row.qty_base == null) continue; // presence-only need unknown → not counted missing (conservative)
        missing++;
      } else if (row.qty_base != null) {
        const fq = fridgeQty.get(row.ingredientId);
        if (fq != null && fq < row.qty_base) missing++;
      }
    }
    if (requireAll && missing > 0) continue;
    out.push({ recipeId: id, missing, enough: missing === 0 });
  }
  return out.sort((a, b) => a.missing - b.missing);
}

export async function matchRecipes(
  fridge: FridgeItem[],
  opts: { requireAll?: boolean; recipeIds?: string[] } = {},
  fetchFn = fetchRecipeIngredients,
): Promise<RankedRecipe[]> {
  const rows = await fetchFn(opts.recipeIds);
  if (opts.requireAll) {
    const ids = fridge.map((f) => f.ingredientId).filter(Boolean) as string[];
    const keep = new Set(
      containingAll(
        ids,
        rows.map((r) => ({ recipe_id: r.recipeId, ingredient_id: r.ingredientId ?? "" })),
      ),
    );
    return rankByMissing(
      rows.filter((r) => keep.has(r.recipeId)),
      fridge,
      true,
    );
  }
  return rankByMissing(rows, fridge);
}
