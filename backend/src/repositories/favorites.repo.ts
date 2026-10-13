import { supabaseAdmin } from "../config/supabase.js";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Client = any;

// false = vua insert; true = da like tu truoc (PK conflict).
export async function addFavorite(userId: string, recipeId: string, client: Client = supabaseAdmin): Promise<boolean> {
  const { error } = await client.from("favorites").insert({ user_id: userId, recipe_id: recipeId });
  if (!error) return false;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  if ((error as any).code === "23505") return true;
  throw error;
}

// true = co xoa (moi tru like_count); false = chua tung like.
export async function removeFavorite(userId: string, recipeId: string, client: Client = supabaseAdmin): Promise<boolean> {
  const { error, count } = await client
    .from("favorites")
    .delete({ count: "exact" })
    .eq("user_id", userId)
    .eq("recipe_id", recipeId);
  if (error) throw error;
  return (count ?? 0) > 0;
}

export async function listFavoriteRecipeIds(userId: string, client: Client = supabaseAdmin): Promise<string[]> {
  const { data, error } = await client
    .from("favorites")
    .select("recipe_id")
    .eq("user_id", userId)
    .order("created_at", { ascending: false });
  if (error) throw error;
  return ((data ?? []) as Array<{ recipe_id: string }>).map((r) => r.recipe_id);
}

// Atomic + san 0 (migration 014 bump_like_count).
export async function bumpLikeCount(recipeId: string, delta: 1 | -1, client: Client = supabaseAdmin): Promise<void> {
  const { error } = await client.rpc("bump_like_count", { rid: recipeId, delta });
  if (error) throw error;
}
