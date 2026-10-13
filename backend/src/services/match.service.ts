import { fetchRecipeIngredients } from "../repositories/recipes.repo.js";
import { fetchIngredients } from "../repositories/recipes.repo.js";
import { resolveIngredientId } from "../lib/ingredients.js";

export interface FridgeItem {
  ingredientId?: string | null;
  name?: string;
  qty_base?: number | null;
}

export interface MissingIngredient {
  ingredientId: string;
  name: string | null;
}

export interface RankedRecipe {
  recipeId: string;
  matched: number;
  missing: number;
  enough: boolean;
  matchedIngredients: MissingIngredient[];
  missingIngredients: MissingIngredient[];
}

// Pure: only recipes overlapping the fridge (matched > 0) are listed;
// sort most-overlap first, fewest-missing second. Null-ingredient rows are skipped.
export function rankByMissing(
  recipeRows: Array<{ recipeId: string; ingredientId: string | null; qty_base: number | null }>,
  fridge: FridgeItem[],
  ings: Array<{ id: string; name: string }> = [],
): RankedRecipe[] {
  const haveIds = new Set(fridge.map((f) => f.ingredientId).filter(Boolean) as string[]);
  const fridgeQty = new Map(
    fridge.filter((f) => f.ingredientId).map((f) => [f.ingredientId as string, f.qty_base ?? null]),
  );
  const names = new Map(ings.map((i) => [i.id, i.name]));
  const byRecipe = new Map<string, typeof recipeRows>();
  for (const r of recipeRows) {
    const l = byRecipe.get(r.recipeId) ?? [];
    l.push(r);
    byRecipe.set(r.recipeId, l);
  }
  const out: RankedRecipe[] = [];
  for (const [id, rows] of byRecipe) {
    let matched = 0;
    let missing = 0;
    const matchedIngredients: MissingIngredient[] = [];
    const missingIngredients: MissingIngredient[] = [];
    for (const row of rows) {
      if (!row.ingredientId) continue; // unknown ingredient, can't judge
      if (!haveIds.has(row.ingredientId)) {
        missing++; // need it, don't have it (qty irrelevant)
        missingIngredients.push({ ingredientId: row.ingredientId, name: names.get(row.ingredientId) ?? null });
      } else if (row.qty_base != null) {
        const fq = fridgeQty.get(row.ingredientId);
        if (fq != null && fq < row.qty_base) {
          missing++; // have it but not enough
          matched++; // still overlap: fridge contains this ingredient
          matchedIngredients.push({ ingredientId: row.ingredientId, name: names.get(row.ingredientId) ?? null });
          missingIngredients.push({ ingredientId: row.ingredientId, name: names.get(row.ingredientId) ?? null });
        } else {
          matched++;
          matchedIngredients.push({ ingredientId: row.ingredientId, name: names.get(row.ingredientId) ?? null });
        }
      } else {
        matched++;
        matchedIngredients.push({ ingredientId: row.ingredientId, name: names.get(row.ingredientId) ?? null });
      }
    }
    if (matched === 0) continue; // no overlap with fridge, not a suggestion
    out.push({ recipeId: id, matched, missing, enough: missing === 0, matchedIngredients, missingIngredients });
  }
  return out.sort((a, b) => b.matched - a.matched || a.missing - b.missing);
}

export async function matchRecipes(
  fridge: FridgeItem[],
  opts: { limit?: number } = {},
  fetchFn = fetchRecipeIngredients,
  fetchIngs = fetchIngredients,
): Promise<RankedRecipe[]> {
  const [rows, ings] = await Promise.all([fetchFn(), fetchIngs()]);
  // Raw input (scan/manual names) -> ingredient_id; unresolvable names stay null (skipped in rank).
  const resolved = fridge.map((f) => ({
    ...f,
    ingredientId: f.ingredientId ?? resolveIngredientId(f.name, ings),
  }));
  // ponytail: ten lay tu fetchRecipeCards o controller; rank khong fetch ten nua
  const ranked = rankByMissing(rows, resolved, ings);
  return opts.limit === undefined ? ranked : ranked.slice(0, opts.limit);
}
