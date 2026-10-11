import { getScan, fetchScanItems, insertFoods, markScanCommitted } from "../repositories/scans.repo.js";
import { fetchIngredients } from "../repositories/recipes.repo.js";
import { resolveIngredientId } from "../lib/ingredients.js";
import type { ScanItemRow } from "../repositories/scans.repo.js";

export interface CommitDeps {
  getScan: typeof getScan;
  fetchItems: typeof fetchScanItems;
  fetchIngredients: typeof fetchIngredients;
  insertFoods: typeof insertFoods;
  markCommitted: typeof markScanCommitted;
}

const defaultDeps: CommitDeps = {
  getScan,
  fetchItems: fetchScanItems,
  fetchIngredients,
  insertFoods,
  markCommitted: markScanCommitted,
};

// Plan A: ingredient_id is assigned at commit time, so foods ARE ingredients.
// Names the OCR/BM25 step already mapped keep their id; raw names resolve here.
// Sequential (supabase-js has no multi-table txn): status flips last, so a
// failure leaves the scan as draft and the commit is safely retryable.
export async function commitScan(scanId: string, deps: CommitDeps = defaultDeps) {
  const scan = await deps.getScan(scanId);
  if (!scan) throw Object.assign(new Error("scan not found"), { status: 404 });
  if (scan.status === "committed") return { inserted: 0, already: true };
  const [items, ings] = await Promise.all([deps.fetchItems(scanId), deps.fetchIngredients()]);
  const resolved: ScanItemRow[] = items.map((i) => ({
    ...i,
    ingredient_id: (i as ScanItemRow).ingredient_id ?? resolveIngredientId(i.name, ings),
  }));
  const inserted = await deps.insertFoods(scan.fridge_id, resolved);
  await deps.markCommitted(scanId);
  return { inserted, already: false };
}
