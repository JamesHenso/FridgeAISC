import { getScan, fetchScanItems, insertFoods, markScanCommitted, createScan, addScanItems, updateScanItem } from "../repositories/scans.repo.js";
import { fetchIngredients } from "../repositories/recipes.repo.js";
import { resolveIngredientId } from "../lib/ingredients.js";
import { toBase } from "../lib/units.js";
import type { ScanItemRow, NewScanItem, ScanMetadata } from "../repositories/scans.repo.js";

export interface ScanDeps {
  getScan: typeof getScan;
  fetchItems: typeof fetchScanItems;
  fetchIngredients: typeof fetchIngredients;
  insertFoods: typeof insertFoods;
  markCommitted: typeof markScanCommitted;
  createScan: typeof createScan;
  addItems: typeof addScanItems;
  updateItem: typeof updateScanItem;
}

const defaultDeps: ScanDeps = {
  getScan,
  fetchItems: fetchScanItems,
  fetchIngredients,
  insertFoods,
  markCommitted: markScanCommitted,
  createScan,
  addItems: addScanItems,
  updateItem: updateScanItem,
};

export const LOCATIONS = ["fridge", "freezer"] as const;

function fillBase(i: ScanItemRow | NewScanItem): ScanItemRow {
  const b = i.qty_base == null ? toBase(i.qty_display ?? null, i.unit_display ?? null) : null;
  return {
    name: i.name,
    ingredient_id: i.ingredient_id ?? null,
    qty_display: i.qty_display ?? null,
    unit_display: i.unit_display ?? null,
    qty_base: i.qty_base ?? b?.qty_base ?? null,
    base_unit: i.base_unit ?? b?.base_unit ?? null,
    purchased_at: i.purchased_at ?? null,
    expiry_date: i.expiry_date ?? null,
    location: i.location ?? null,
    ...(i.raw_name !== undefined ? { raw_name: i.raw_name } : {}),
    ...(i.unit_price_vnd !== undefined ? { unit_price_vnd: i.unit_price_vnd } : {}),
    ...(i.price_vnd !== undefined ? { price_vnd: i.price_vnd } : {}),
    ...(i.source_type !== undefined ? { source_type: i.source_type } : {}),
    ...(i.match_status !== undefined ? { match_status: i.match_status } : {}),
  };
}

function fail(status: number, message: string): never {
  throw Object.assign(new Error(message), { status });
}

// Nhập tay thay OCR: items đã có ingredient_id do UI match ngầm, chỉ thiếu qty/ngày thì user sửa sau.
export async function createScanWithItems(fridgeId: string, items: NewScanItem[], deps: ScanDeps = defaultDeps, metadata: ScanMetadata = {}) {
  if (!fridgeId) fail(400, "fridge_id required");
  if (!items?.length) fail(400, "items required");
  for (const i of items) {
    if (!i.name?.trim()) fail(400, "item.name required");
    if (i.price_vnd != null && (!Number.isFinite(i.price_vnd) || i.price_vnd < 0)) fail(400, "invalid price_vnd");
    if (i.unit_price_vnd != null && (!Number.isFinite(i.unit_price_vnd) || i.unit_price_vnd < 0)) fail(400, "invalid unit_price_vnd");
  }
  const scanId = await deps.createScan(fridgeId, metadata);
  const rows = items.map(fillBase);
  const created = await deps.addItems(scanId, rows);
  return { scanId, inserted: created.length, items: created };
}

// Xác nhận 1: sửa số lượng / ngày mua / hạn dùng hàng loạt.
export async function updateScanItems(
  scanId: string,
  items: Array<{ id: string; name?: string; ingredient_id?: string | null; qty_display?: number | null; unit_display?: string | null; qty_base?: number | null; base_unit?: string | null; purchased_at?: string | null; expiry_date?: string | null; raw_name?: string | null; unit_price_vnd?: number | null; price_vnd?: number | null; source_type?: string | null; match_status?: string | null }>,
  deps: ScanDeps = defaultDeps,
) {
  const scan = await deps.getScan(scanId);
  if (!scan) fail(404, "scan not found");
  if (scan!.status === "committed") fail(400, "scan already committed");
  if (!items?.length) return { updated: 0 };
  for (const i of items) {
    if (!i.id) fail(400, "item.id required");
    const patch: Record<string, number | string | null> = {};
    for (const k of ["name", "ingredient_id", "qty_display", "unit_display", "qty_base", "base_unit", "purchased_at", "expiry_date", "raw_name", "unit_price_vnd", "price_vnd", "source_type", "match_status"] as const) {
      if (i[k] !== undefined) patch[k] = i[k];
    }
    if (patch.name !== undefined && !String(patch.name).trim()) fail(400, "item.name required");
    for (const key of ["price_vnd", "unit_price_vnd"] as const) {
      if (patch[key] != null && (!Number.isFinite(patch[key]) || (patch[key] as number) < 0)) fail(400, `invalid ${key}`);
    }
    if ((patch.qty_display !== undefined || patch.unit_display !== undefined) && patch.qty_base === undefined) {
      const b = toBase(
        (patch.qty_display ?? null) as number | null,
        (patch.unit_display ?? null) as string | null,
      );
      patch.qty_base = b.qty_base;
      patch.base_unit = b.base_unit;
    }
    await deps.updateItem(i.id, scanId, patch);
  }
  return { updated: items.length };
}

// Xác nhận 2 (drag-drop): xếp ngăn hàng loạt.
export async function updateScanLocations(scanId: string, items: Array<{ id: string; location: string }>, deps: ScanDeps = defaultDeps) {
  const scan = await deps.getScan(scanId);
  if (!scan) fail(404, "scan not found");
  if (scan!.status === "committed") fail(400, "scan already committed");
  if (!items?.length) fail(400, "items required");
  for (const i of items) {
    if (!i.id) fail(400, "item.id required");
    if (!(LOCATIONS as readonly string[]).includes(i.location)) fail(400, `invalid location for ${i.id}`);
    await deps.updateItem(i.id, scanId, { location: i.location });
  }
  return { updated: items.length };
}

// Plan A: ingredient_id pass-through từ nhập tay/scan; resolve chỉ là fallback cho đồ cũ thiếu id.
// Sequential (supabase-js has no multi-table txn): status flips last, fail thì vẫn draft, retry được.
export async function commitScan(scanId: string, deps: ScanDeps = defaultDeps) {
  const scan = await deps.getScan(scanId);
  if (!scan) fail(404, "scan not found");
  if (scan!.status === "committed") return { inserted: 0, already: true };
  const [items, ings] = await Promise.all([deps.fetchItems(scanId), deps.fetchIngredients()]);
  const resolved: ScanItemRow[] = items.map((i) => ({
    ...fillBase(i),
    ingredient_id: i.ingredient_id ?? resolveIngredientId(i.name, ings),
  }));
  const unmapped = resolved.filter((i) => !i.ingredient_id);
  if (unmapped.length) {
    fail(400, `ingredient required: ${unmapped.map((i) => i.id ?? i.name).join(",")}`);
  }
  const inserted = await deps.insertFoods(scan!.fridge_id, resolved);
  await deps.markCommitted(scanId);
  return { inserted, already: false };
}
