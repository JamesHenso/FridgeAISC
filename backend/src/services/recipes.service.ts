import {
  fetchIngredientNames,
  fetchRecipe,
  fetchRecipeCards,
  fetchRecipeDetail,
  fetchRecipeDetailsByIds,
  fetchRecipeRowIngredients,
  fetchReelIds,
  type RecipeCard,
  type RecipeDetailRow,
} from "../repositories/recipes.repo.js";
import { rankByMissing, type FridgeItem, type MissingIngredient } from "../services/match.service.js";
import { resolveIngredientId, type IngredientEntry } from "../lib/ingredients.js";

export interface RecipeIngredientView {
  image_url?: string | null;
  category?: string | null;
  ingredientId: string | null;
  name: string | null;
  quantity: string | null;
  unit: string | null;
  qty_base: number | null;
  base_unit: string | null;
}

// List card chung cho match + search + favorites: info co ban + servings/time/difficulty tu RecipeDetail.
export async function attachDetails<T extends RecipeCard>(
  cards: T[],
  fetchD = fetchRecipeDetailsByIds,
): Promise<Array<T & { servings: number | null; time: number | null; difficulty: string | null }>> {
  const map = new Map((await fetchD(cards.map((c) => c.id))).map((d) => [d.recipeId, d]));
  return cards.map((c) => {
    const d = map.get(c.id);
    return { ...c, servings: d?.servings ?? null, time: d?.time ?? null, difficulty: d?.difficulty ?? null };
  });
}

export type ReelCard = RecipeCard & { servings: number | null; time: number | null; difficulty: string | null };

// ponytail: shuffle app-side; chuyen sang RPC order-random khi bang Recipe lon den muc page id ton kem
export async function getReels(
  limit = 10,
  fetchIds = fetchReelIds,
  fetchCards = fetchRecipeCards,
  fetchD = fetchRecipeDetailsByIds,
  pick: (ids: string[], n: number) => string[] = (ids, n) =>
    [...ids].sort(() => Math.random() - 0.5).slice(0, n),
): Promise<ReelCard[]> {
  const ids = pick(await fetchIds(), Math.min(limit, 50));
  return attachDetails(await fetchCards(ids), fetchD);
}

export interface HaveNeed {
  matched: MissingIngredient[];
  missing: MissingIngredient[];
}

export interface FullRecipe {
  info: RecipeCard;
  ingredients: RecipeIngredientView[];
  detail: RecipeDetailRow | null;
  haveNeed?: HaveNeed;
}

// Chi tiet mon: info + nguyen lieu (ten chuan catalog thang, fallback ten denormalized) + steps.
// Co fridge thi kem haveNeed (tai dung rankByMissing cho dung 1 recipe).
export async function getRecipe(
  recipeId: string,
  fetchFn = fetchRecipe,
  fetchDetail = fetchRecipeDetail,
  fetchRows = fetchRecipeRowIngredients,
  fetchNames = fetchIngredientNames,
  fridge?: { items: FridgeItem[]; ings: IngredientEntry[] },
): Promise<FullRecipe> {
  const info = await fetchFn(recipeId);
  if (!info) throw Object.assign(new Error(`recipe ${recipeId} not found`), { status: 404 });
  const [rows, detail] = await Promise.all([fetchRows(recipeId), fetchDetail(recipeId)]);
  const ids = [...new Set(rows.map((r) => r.ingredientId).filter(Boolean) as string[])];
  const catalog = new Map((await fetchNames(ids)).map((n) => [n.id, n]));
  const names = new Map([...catalog].map(([id, row]) => [id, row.name]));
  const out: FullRecipe = {
    info,
    ingredients: rows.map((r) => ({
      ...r,
      name: (r.ingredientId && names.get(r.ingredientId)) || r.name,
      image_url: r.ingredientId ? catalog.get(r.ingredientId)?.image_url ?? null : null,
      category: r.ingredientId ? catalog.get(r.ingredientId)?.category ?? null : null,
    })),
    detail,
  };
  if (fridge) {
    const resolved = fridge.items.map((f) => ({
      ...f,
      ingredientId: f.ingredientId ?? resolveIngredientId(f.name, fridge.ings),
    }));
    const [r] = rankByMissing(
      rows.map((row) => ({ recipeId, ingredientId: row.ingredientId, qty_base: row.qty_base, base_unit: row.base_unit })),
      resolved,
      fridge.ings,
    );
    out.haveNeed = r
      ? { matched: r.matchedIngredients, missing: r.missingIngredients }
      : {
          matched: [],
          missing: rows
            .filter((row) => row.ingredientId)
            .map((row) => ({ ingredientId: row.ingredientId as string, name: names.get(row.ingredientId as string) ?? row.name })),
        };
  }
  return out;
}
