import { supabaseAdmin } from "../config/supabase.js";
import type { FridgeItem } from "../services/match.service.js";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function fetchFoods(fridgeId: string, client: any = supabaseAdmin): Promise<FridgeItem[]> {
  const { data, error } = await client
    .from("foods")
    .select("ingredient_id,qty_base")
    .eq("fridge_id", fridgeId);
  if (error) throw error;
  return ((data ?? []) as Array<{ ingredient_id: string | null; qty_base: number | null }>).map((f) => ({
    ingredientId: f.ingredient_id,
    qty_base: f.qty_base,
  }));
}
