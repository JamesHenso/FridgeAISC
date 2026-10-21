import { supabaseAdmin } from "../config/supabase.js";
import type { FridgeItem } from "../services/match.service.js";
import { toBase } from "../lib/units.js";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Client = any;

export async function fetchFoods(fridgeId: string, client: Client = supabaseAdmin): Promise<FridgeItem[]> {
  const { data, error } = await client
    .from("foods")
    .select("ingredient_id,name,qty_base,base_unit,expiry_date")
    .eq("fridge_id", fridgeId);
  if (error) throw error;
  return ((data ?? []) as Array<{ ingredient_id: string | null; name: string; qty_base: number | null; base_unit: string | null; expiry_date: string | null }>).map((f) => ({
    ingredientId: f.ingredient_id,
    name: f.name,
    qty_base: f.qty_base,
    base_unit: f.base_unit,
    expiry_date: f.expiry_date,
  }));
}

export interface FoodRow {
  id: string;
  name: string;
  ingredient_id: string | null;
  qty_display: number | null;
  unit_display: string | null;
  qty_base: number | null;
  base_unit: string | null;
  purchased_at: string | null;
  expiry_date: string | null;
  location: string | null;
  unit_price_vnd: number | null;
  price_vnd: number | null;
  image_url?: string | null;
  category?: string | null;
  source_type?: string | null;
}

export async function listFoods(fridgeId: string, location?: string, q?: string, client: Client = supabaseAdmin): Promise<FoodRow[]> {
  const select = "id,name,ingredient_id,qty_display,unit_display,qty_base,base_unit,purchased_at,expiry_date,location,unit_price_vnd,price_vnd,source_type";
  let qy = client
    .from("foods")
    .select(select)
    .eq("fridge_id", fridgeId)
    .order("expiry_date", { ascending: true, nullsFirst: false });
  if (location) qy = qy.eq("location", location);
  if (q?.trim()) qy = qy.ilike("name", `%${q.trim()}%`);
  const { data, error } = await qy;
  if (error) throw error;
  const foods = (data ?? []) as FoodRow[];
  const ingredientIds = [...new Set(foods.map((food) => food.ingredient_id).filter((id): id is string => Boolean(id)))];
  if (ingredientIds.length === 0) return foods;

  // Ingredient thumbnails are optional; if the catalog query fails inventory still loads.
  const { data: ingredients, error: catalogError } = await client
    .from("Ingredients").select("id,image_url,category").in("id", ingredientIds);
  if (catalogError) return foods;
  const catalog = new Map(((ingredients ?? []) as Array<{ id: string; image_url: string | null; category: string | null }>)
    .map((ingredient) => [ingredient.id, ingredient]));
  return foods.map((food) => ({
    ...food,
    category: food.ingredient_id ? catalog.get(food.ingredient_id)?.category ?? null : null,
    image_url: food.ingredient_id ? catalog.get(food.ingredient_id)?.image_url ?? null : null,
  }));
}

export interface InventoryStats {
  inStock: number;
  useToday: number;
  useSoon: number;
  cleanOut: number;
}
// ponytail: day-bucket compare on YYYY-MM-DD strings; null expiry counts only in inStock
export function inventoryStats(rows: Array<{ expiry_date: string | null }>, today = new Date()): InventoryStats {
  const day = (d: Date) => d.toISOString().slice(0, 10);
  const t = day(today);
  const soon = new Set([1, 2, 3].map((n) => day(new Date(today.getTime() + n * 86400000))));
  let useToday = 0, useSoon = 0, cleanOut = 0;
  for (const r of rows) {
    if (!r.expiry_date) continue;
    const e = r.expiry_date.slice(0, 10);
    if (e < t) cleanOut++;
    else if (e === t) useToday++;
    else if (soon.has(e)) useSoon++;
  }
  return { inStock: rows.length - cleanOut, useToday, useSoon, cleanOut };
}

function notFound(what: string): never {
  throw Object.assign(new Error(`${what} not found`), { status: 404 });
}

export type FoodPatch = Partial<Pick<FoodRow, "name" | "qty_display" | "unit_display" | "qty_base" | "base_unit" | "purchased_at" | "expiry_date" | "location">>;

// ponytail: validation + toBase fill live here (no service layer for single-table ops); controllers stay thin
export async function updateFood(id: string, patch: FoodPatch, client: Client = supabaseAdmin): Promise<void> {
  if (patch.location !== undefined && patch.location !== "fridge" && patch.location !== "freezer")
    throw Object.assign(new Error(`invalid location for ${id}`), { status: 400 });
  const out: Record<string, unknown> = { ...patch };
  if ((patch.qty_display !== undefined || patch.unit_display !== undefined) && patch.qty_base === undefined) {
    const b = toBase(patch.qty_display ?? null, patch.unit_display ?? null);
    out.qty_base = b.qty_base;
    out.base_unit = b.base_unit;
  }
  const { error, count } = await client.from("foods").update(out, { count: "exact" }).eq("id", id);
  if (error) throw error;
  if (!count) notFound(`food ${id}`);
}

export async function deleteFood(id: string, client: Client = supabaseAdmin): Promise<void> {
  const { error, count } = await client.from("foods").delete({ count: "exact" }).eq("id", id);
  if (error) throw error;
  if (!count) notFound(`food ${id}`);
}

export async function renameFridge(id: string, name: string, client: Client = supabaseAdmin): Promise<void> {
  if (!name?.trim()) throw Object.assign(new Error("name required"), { status: 400 });
  const { error, count } = await client.from("fridges").update({ name: name.trim() }, { count: "exact" }).eq("id", id);
  if (error) throw error;
  if (!count) notFound(`fridge ${id}`);
}

export interface FridgeListItem {
  id: string;
  name: string;
  invite_code: string | null;
  role: string | null;
}

export async function createFridge(ownerId: string, name: string, client: Client = supabaseAdmin): Promise<FridgeListItem> {
  if (!name?.trim()) throw Object.assign(new Error("name required"), { status: 400 });
  const { data, error } = await client
    .from("fridges")
    .insert({ owner_id: ownerId, name: name.trim() })
    .select("id,name,invite_code")
    .single();
  if (error) throw error;
  return { ...(data as { id: string; name: string; invite_code: string | null }), role: "owner" };
}

// Setting switch tu + Family: tu toi so huu + tham gia, kem role.
export async function listFridges(userId: string, client: Client = supabaseAdmin): Promise<FridgeListItem[]> {
  const { data: mems, error: e1 } = await client.from("fridge_members").select("fridge_id,role").eq("user_id", userId);
  if (e1) throw e1;
  const roles = new Map(((mems ?? []) as Array<{ fridge_id: string; role: string | null }>).map((m) => [m.fridge_id, m.role]));
  const { data: owned, error: e2 } = await client.from("fridges").select("id").eq("owner_id", userId);
  if (e2) throw e2;
  for (const o of (owned ?? []) as Array<{ id: string }>) if (!roles.has(o.id)) roles.set(o.id, "owner");
  if (roles.size === 0) return [];
  const { data, error } = await client.from("fridges").select("id,name,invite_code").in("id", [...roles.keys()]);
  if (error) throw error;
  return ((data ?? []) as Array<{ id: string; name: string; invite_code: string | null }>)
    .map((f) => ({ ...f, role: roles.get(f.id) ?? null }))
    .sort((a, b) => a.name.localeCompare(b.name));
}

export interface FridgeMember {
  user_id: string;
  role: string | null;
  name: string | null;
  email: string | null;
}

export async function listMembers(fridgeId: string, client: Client = supabaseAdmin): Promise<FridgeMember[]> {
  const { data: mems, error: e1 } = await client.from("fridge_members").select("user_id,role").eq("fridge_id", fridgeId);
  if (e1) throw e1;
  const rows = (mems ?? []) as Array<{ user_id: string; role: string | null }>;
  if (rows.length === 0) return [];
  const roles = new Map(rows.map((m) => [m.user_id, m.role]));
  const { data: profs, error: e2 } = await client.from("profiles").select("id,name,email").in("id", [...roles.keys()]);
  if (e2) throw e2;
  return ((profs ?? []) as Array<{ id: string; name: string | null; email: string | null }>).map((p) => ({
    user_id: p.id,
    role: roles.get(p.id) ?? null,
    name: p.name,
    email: p.email,
  }));
}

export async function joinFridgeByCode(
  userId: string,
  code: string,
  client: Client = supabaseAdmin,
): Promise<{ fridgeId: string; already: boolean }> {
  if (!code?.trim()) throw Object.assign(new Error("code required"), { status: 400 });
  const { data: fr, error } = await client.from("fridges").select("id").eq("invite_code", code.trim()).maybeSingle();
  if (error) throw error;
  if (!fr) throw Object.assign(new Error("invalid code"), { status: 404 });
  const fridgeId = (fr as { id: string }).id;
  const { error: e2 } = await client.from("fridge_members").insert({ fridge_id: fridgeId, user_id: userId, role: "member" });
  if (!e2) return { fridgeId, already: false };
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  if ((e2 as any).code === "23505") return { fridgeId, already: true };
  throw e2;
}
