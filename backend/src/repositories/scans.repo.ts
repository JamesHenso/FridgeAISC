import { supabaseAdmin } from "../config/supabase.js";

export interface ScanRow {
  id: string;
  fridge_id: string;
  status: string;
}

export interface ScanItemRow {
  name: string;
  qty_display: number | null;
  unit_display: string | null;
  qty_base: number | null;
  base_unit: string | null;
  expiry_date: string | null;
  location: string | null;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Client = any;

export async function getScan(scanId: string, client: Client = supabaseAdmin): Promise<ScanRow | null> {
  const { data, error } = await client.from("scans").select("id,fridge_id,status").eq("id", scanId).maybeSingle();
  if (error) throw error;
  return (data ?? null) as ScanRow | null;
}

export async function fetchScanItems(scanId: string, client: Client = supabaseAdmin): Promise<ScanItemRow[]> {
  const { data, error } = await client
    .from("scan_items")
    .select("name,qty_display,unit_display,qty_base,base_unit,expiry_date,location")
    .eq("scan_id", scanId);
  if (error) throw error;
  return (data ?? []) as ScanItemRow[];
}

export async function insertFoods(fridgeId: string, items: ScanItemRow[], client: Client = supabaseAdmin): Promise<number> {
  if (items.length === 0) return 0;
  const rows = items.map((i) => ({
    fridge_id: fridgeId,
    name: i.name,
    qty_display: i.qty_display,
    unit_display: i.unit_display,
    qty_base: i.qty_base,
    base_unit: i.base_unit,
    expiry_date: i.expiry_date,
  }));
  const { error, count } = await client.from("foods").insert(rows, { count: "exact" });
  if (error) throw error;
  return count ?? rows.length;
}

export async function markScanCommitted(scanId: string, client: Client = supabaseAdmin): Promise<void> {
  const { error } = await client.from("scans").update({ status: "committed" }).eq("id", scanId);
  if (error) throw error;
}
