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
export async function fetchRecipeIngredients(client: any = supabaseAdmin): Promise<RecipeIngredientRow[]> {
  const q = client.from("RecipeIngredient").select("recipeId,ingredientId,qty_base,base_unit").order("recipeId");
  return (await pageAll(q)) as RecipeIngredientRow[];
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function fetchIngredients(client: any = supabaseAdmin): Promise<Array<{ id: string; name: string }>> {
  const q = client.from("Ingredients").select("id,name").order("name");
  return (await pageAll(q)) as Array<{ id: string; name: string }>;
}

export interface RecipeCard {
  id: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  category: string | null;
  method: string | null;
  like_count: number | null;
  video_id: string | null;
  video_provider: string | null;
}

const CARD_SELECT = "id,name,description,imageUrl,category,method,like_count,video_id,video_provider";

export async function searchRecipes(
  opts: { category?: string; method?: string; name?: string; limit?: number } = {},
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  client: any = supabaseAdmin,
): Promise<RecipeCard[]> {
  let q = client
    .from("Recipe")
    .select(CARD_SELECT)
    .order("name");
  if (opts.category) q = q.eq("category", opts.category);
  if (opts.method) q = q.eq("method", opts.method);
  if (opts.name?.trim()) q = q.ilike("name", `%${opts.name.trim()}%`);
  const { data, error } = await q.limit(opts.limit ?? 10);
  if (error) throw error;
  return (data ?? []) as RecipeCard[];
}

export async function fetchRecipe(
  id: string,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  client: any = supabaseAdmin,
): Promise<RecipeCard | null> {
  const { data, error } = await client
    .from("Recipe")
    .select(CARD_SELECT)
    .eq("id", id)
    .maybeSingle();
  if (error) throw error;
  return (data ?? null) as RecipeCard | null;
}

// ponytail: batched card lookup preserving input (rank) order
export async function fetchRecipeCards(
  ids: string[],
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  client: any = supabaseAdmin,
): Promise<RecipeCard[]> {
  if (ids.length === 0) return [];
  const { data, error } = await client
    .from("Recipe")
    .select(CARD_SELECT)
    .in("id", ids);
  if (error) throw error;
  const map = new Map(((data ?? []) as RecipeCard[]).map((r) => [r.id, r]));
  return ids.map((id) => map.get(id)).filter((r) => r !== undefined);
}

export async function fetchRecipeMethods(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  client: any = supabaseAdmin,
): Promise<string[]> {
  const q = client.from("Recipe").select("method").not("method", "is", null).order("method");
  const methods = ((await pageAll(q)) as Array<{ method: string | null }>)
    .map((row) => row.method?.trim())
    .filter((method): method is string => Boolean(method));
  return [...new Set(methods)];
}

export async function fetchRecipeIdsByMethod(method: string): Promise<string[]> {
  const q = supabaseAdmin.from("Recipe").select("id").eq("method", method);
  return ((await pageAll(q)) as Array<{ id: string }>).map((row) => row.id);
}

export interface RecipeDetailRow {
  recipeId?: string;
  servings: number | null;
  time: number | null;
  difficulty: string | null;
  videoUrl: string | null;
  preparation: string | null;
  instructions: string | null;
  servingText: string | null;
}

export async function fetchRecipeDetail(
  recipeId: string,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  client: any = supabaseAdmin,
): Promise<RecipeDetailRow | null> {
  const { data, error } = await client
    .from("RecipeDetail")
    .select("servings,time,difficulty,videoUrl,preparation,instructions,servingText")
    .eq("recipeId", recipeId)
    .maybeSingle();
  if (error) throw error;
  return (data ?? null) as RecipeDetailRow | null;
}

// ponytail: one .in query for card enrichment; recipeId selected for merge key
export async function fetchRecipeDetailsByIds(
  recipeIds: string[],
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  client: any = supabaseAdmin,
): Promise<RecipeDetailRow[]> {
  if (recipeIds.length === 0) return [];
  const { data, error } = await client
    .from("RecipeDetail")
    .select("recipeId,servings,time,difficulty")
    .in("recipeId", recipeIds);
  if (error) throw error;
  return (data ?? []) as RecipeDetailRow[];
}

export interface RecipeRowIngredient {
  ingredientId: string | null;
  name: string | null;
  quantity: string | null;
  unit: string | null;
  qty_base: number | null;
  base_unit: string | null;
}

export async function fetchRecipeRowIngredients(
  recipeId: string,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  client: any = supabaseAdmin,
): Promise<RecipeRowIngredient[]> {
  const { data, error } = await client
    .from("RecipeIngredient")
    .select("ingredientId,name,quantity,unit,qty_base,base_unit")
    .eq("recipeId", recipeId)
    .order("name");
  if (error) throw error;
  return (data ?? []) as RecipeRowIngredient[];
}

// ponytail: single batched name lookup instead of reusing whole-table fetchIngredients for one recipe
export async function fetchIngredientNames(
  ids: string[],
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  client: any = supabaseAdmin,
): Promise<Array<{ id: string; name: string }>> {
  if (ids.length === 0) return [];
  const { data, error } = await client.from("Ingredients").select("id,name").in("id", ids);
  if (error) throw error;
  return (data ?? []) as Array<{ id: string; name: string }>;
}

// Reels: id mon co video (random tinh app-side o service).
export async function fetchReelIds(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  client: any = supabaseAdmin,
): Promise<string[]> {
  const q = client.from("Recipe").select("id").not("video_id", "is", null);
  return ((await pageAll(q)) as Array<{ id: string }>).map((r) => r.id);
}
