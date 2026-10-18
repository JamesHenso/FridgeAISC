import { fetchRecipeIngredients } from "../repositories/recipes.repo.js";
import { fetchIngredients } from "../repositories/recipes.repo.js";
import { resolveIngredientId } from "../lib/ingredients.js";

export interface FridgeItem {
  ingredientId?: string | null;
  name?: string;
  qty_base?: number | null;
  base_unit?: string | null;
  expiry_date?: string | null;
}

export interface MissingIngredient {
  ingredientId: string;
  name: string | null;
}

export interface RankedRecipe {
  recipeId: string;
  matched: number;
  missing: number;
  urgencyScore: number;
  enough: boolean;
  matchedIngredients: MissingIngredient[];
  missingIngredients: MissingIngredient[];
}

// Ranks recipes by ingredients that can use non-expired stock (with extra weight
// for near-expiry lots), then fewer missing ingredients. Unknown recipe rows are skipped.
export function rankByMissing(
  recipeRows: Array<{ recipeId: string; ingredientId: string | null; qty_base: number | null; base_unit?: string | null }>,
  fridge: FridgeItem[],
  ings: Array<{ id: string; name: string }> = [],
  today = new Date().toISOString().slice(0, 10),
): RankedRecipe[] {
  const stock = new Map<string, Array<{ qty: number | null; unit: string | null; expiry: string | null; urgency: number }>>();
  for (const item of fridge) {
    if (!item.ingredientId) continue;
    const expiry = item.expiry_date?.slice(0, 10) ?? null;
    if (expiry && expiry < today) continue; // expired lots are not cookable stock
    const days = expiry ? Math.round((Date.parse(`${expiry}T00:00:00Z`) - Date.parse(`${today}T00:00:00Z`)) / 86400000) : null;
    const urgency = days == null ? 0 : days <= 1 ? 3 : days <= 3 ? 2 : days <= 7 ? 1 : 0;
    const lots = stock.get(item.ingredientId) ?? [];
    lots.push({ qty: item.qty_base ?? null, unit: item.base_unit ?? null, expiry, urgency });
    stock.set(item.ingredientId, lots);
  }
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
    let urgencyScore = 0;
    const countedUrgentIngredients = new Set<string>();
    const matchedIngredients: MissingIngredient[] = [];
    const missingIngredients: MissingIngredient[] = [];
    for (const row of rows) {
      if (!row.ingredientId) continue; // unknown ingredient, can't judge
      const lots = stock.get(row.ingredientId);
      if (!lots?.length) {
        missing++; // need it, don't have it (qty irrelevant)
        missingIngredients.push({ ingredientId: row.ingredientId, name: names.get(row.ingredientId) ?? null });
      } else {
        const quantitiesComparable = row.qty_base != null && row.base_unit != null &&
          lots.every((lot) => lot.qty != null && lot.unit === row.base_unit);
        const totalQty = quantitiesComparable ? lots.reduce((sum, lot) => sum + (lot.qty ?? 0), 0) : null;
        if (!countedUrgentIngredients.has(row.ingredientId)) {
          if (quantitiesComparable && row.qty_base! > 0) {
            let leftToAllocate = row.qty_base!;
            for (const lot of [...lots].sort((a, b) => (a.expiry ?? "9999-12-31").localeCompare(b.expiry ?? "9999-12-31"))) {
              const used = Math.min(leftToAllocate, lot.qty ?? 0);
              urgencyScore += lot.urgency * used / row.qty_base!;
              leftToAllocate -= used;
              if (leftToAllocate <= 0) break;
            }
          } else {
            urgencyScore += Math.max(...lots.map((lot) => lot.urgency));
          }
        }
        countedUrgentIngredients.add(row.ingredientId);
        if (totalQty != null && totalQty < row.qty_base!) {
          missing++; // have it but not enough
          matched++; // still overlap: fridge contains this ingredient
          matchedIngredients.push({ ingredientId: row.ingredientId, name: names.get(row.ingredientId) ?? null });
          missingIngredients.push({ ingredientId: row.ingredientId, name: names.get(row.ingredientId) ?? null });
        } else {
          matched++;
          matchedIngredients.push({ ingredientId: row.ingredientId, name: names.get(row.ingredientId) ?? null });
        }
      }
    }
    if (matched === 0 && missing === 0) continue; // no mapped ingredient rows to evaluate
    out.push({ recipeId: id, matched, missing, urgencyScore, enough: missing === 0, matchedIngredients, missingIngredients });
  }
  return out.sort((a, b) => b.urgencyScore - a.urgencyScore || a.missing - b.missing || b.matched - a.matched);
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
