import { supabaseAdmin } from "../config/supabase.js";

export interface RecipeIngredientRow {
  recipeId: string;
  ingredientId: string | null;
  qty_base: number | null;
  base_unit: string | null;
}

// PostgREST defaults to 1000 rows/page: page through large crawl tables.
async function pageAll(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  base: any,
  pageSize = 1000,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
): Promise<any[]> {
  const out = [];
  for (let offset = 0; ; offset += pageSize) {
    const { data, error } = await base.range(offset, offset + pageSize - 1);
    if (error) throw error;
    out.push(...(data ?? []));
    if ((data ?? []).length < pageSize) break;
  }
  return out;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function fetchRecipeIngredients(recipeIds?: string[], client: any = supabaseAdmin): Promise<RecipeIngredientRow[]> {
  let q = client.from("RecipeIngredient").select("recipeId,ingredientId,qty_base,base_unit").order("recipeId");
  if (recipeIds?.length) q = q.in("recipeId", recipeIds);
  return (await pageAll(q)) as RecipeIngredientRow[];
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function fetchIngredients(client: any = supabaseAdmin): Promise<Array<{ id: string; name: string }>> {
  const q = client.from("Ingredients").select("id,name").order("name");
  return (await pageAll(q)) as Array<{ id: string; name: string }>;
}
