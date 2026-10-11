import { supabaseAdmin } from "../config/supabase.js";

export interface RecipeIngredientRow {
  recipeId: string;
  ingredientId: string | null;
  qty_base: number | null;
  base_unit: string | null;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function fetchRecipeIngredients(recipeIds?: string[], client: any = supabaseAdmin): Promise<RecipeIngredientRow[]> {
  let q = client.from("RecipeIngredient").select("recipeId,ingredientId,qty_base,base_unit");
  if (recipeIds?.length) q = q.in("recipeId", recipeIds);
  const { data, error } = await q;
  if (error) throw error;
  return (data ?? []) as RecipeIngredientRow[];
}
