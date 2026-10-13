import { supabaseAdmin } from "../config/supabase.js";

export interface ScanRow {
  id: string;
  fridge_id: string;
  status: string;
}

export interface ScanItemRow {
  id?: string;
  name: string;
  ingredient_id?: string | null;
  qty_display: number | null;
  unit_display: string | null;
  qty_base: number | null;
  base_unit: string | null;
  purchased_at: string | null;
  expiry_date: string | null;
  location: string | null;
}

export interface NewScanItem {
  name: string;
  ingredient_id?: string | null;
  qty_display?: number | null;
  unit_display?: string | null;
  qty_base?: number | null;
  base_unit?: string | null;
  purchased_at?: string | null;
  expiry_date?: string | null;
  location?: string | null;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Client = any;

export async function getScan(scanId: string, client: Client = supabaseAdmin): Promise<ScanRow | null> {
  const { data, error } = await client.from("scans").select("id,fridge_id,status").eq("id", scanId).maybeSingle();
  if (error) throw error;
  return (data ?? null) as ScanRow | null;
}

export async function createScan(fridgeId: string, client: Client = supabaseAdmin): Promise<string> {
  const { data, error } = await client.from("scans").insert({ fridge_id: fridgeId }).select("id").single();
  if (error) throw error;
  return (data as { id: string }).id;
}

export async function addScanItems(scanId: string, items: ScanItemRow[], client: Client = supabaseAdmin): Promise<Array<{ id: string; name: string }>> {
  if (items.length === 0) return [];
  const rows = items.map((i) => ({
    scan_id: scanId,
    name: i.name,
    ingredient_id: i.ingredient_id ?? null,
    qty_display: i.qty_display ?? null,
    unit_display: i.unit_display ?? null,
    qty_base: i.qty_base ?? null,
    base_unit: i.base_unit ?? null,
    purchased_at: i.purchased_at ?? null,
    expiry_date: i.expiry_date ?? null,
    // ponytail: explicit null beats column DEFAULT 'fridge' so locate step stays mandatory
    location: i.location ?? null,
  }));
  const { data, error } = await client.from("scan_items").insert(rows).select("id,name");
  if (error) throw error;
  return (data ?? []) as Array<{ id: string; name: string }>;
}

export async function fetchScanItems(scanId: string, client: Client = supabaseAdmin): Promise<ScanItemRow[]> {
  const { data, error } = await client
    .from("scan_items")
    .select("id,name,ingredient_id,qty_display,unit_display,qty_base,base_unit,purchased_at,expiry_date,location")
    .eq("scan_id", scanId);
  if (error) throw error;
  return (data ?? []) as ScanItemRow[];
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function updateScanItem(itemId: string, scanId: string, patch: Record<string, any>, client: Client = supabaseAdmin): Promise<void> {
  const { error, count } = await client.from("scan_items").update(patch, { count: "exact" }).eq("id", itemId).eq("scan_id", scanId);
  if (error) throw error;
  // ponytail: count check turns wrong-id/wrong-scan silent no-ops into explicit 404s
  if (!count) throw Object.assign(new Error(`item ${itemId} not found in scan ${scanId}`), { status: 404 });
}

export async function insertFoods(fridgeId: string, items: ScanItemRow[], client: Client = supabaseAdmin): Promise<number> {
  if (items.length === 0) return 0;
  const rows = items.map((i) => ({
    fridge_id: fridgeId,
    ingredient_id: i.ingredient_id ?? null,
    name: i.name,
    qty_display: i.qty_display,
    unit_display: i.unit_display,
    qty_base: i.qty_base,
    base_unit: i.base_unit,
    purchased_at: i.purchased_at ?? null,
    expiry_date: i.expiry_date,
    location: i.location ?? "fridge",
  }));
  const { error, count } = await client.from("foods").insert(rows, { count: "exact" });
  if (error) throw error;
  return count ?? rows.length;
}

export async function markScanCommitted(scanId: string, client: Client = supabaseAdmin): Promise<void> {
  const { error } = await client.from("scans").update({ status: "committed" }).eq("id", scanId);
  if (error) throw error;
}
