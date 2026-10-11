import { getScan, fetchScanItems, insertFoods, markScanCommitted } from "../repositories/scans.repo.js";
import type { ScanItemRow } from "../repositories/scans.repo.js";

export interface CommitDeps {
  getScan: typeof getScan;
  fetchItems: typeof fetchScanItems;
  insertFoods: typeof insertFoods;
  markCommitted: typeof markScanCommitted;
}

const defaultDeps: CommitDeps = { getScan, fetchItems: fetchScanItems, insertFoods, markCommitted: markScanCommitted };

// Sequential (supabase-js has no multi-table txn): status flips last, so a
// failure leaves the scan as draft and the commit is safely retryable.
export async function commitScan(scanId: string, deps: CommitDeps = defaultDeps) {
  const scan = await deps.getScan(scanId);
  if (!scan) throw Object.assign(new Error("scan not found"), { status: 404 });
  if (scan.status === "committed") return { inserted: 0, already: true };
  const items: ScanItemRow[] = await deps.fetchItems(scanId);
  const inserted = await deps.insertFoods(scan.fridge_id, items);
  await deps.markCommitted(scanId);
  return { inserted, already: false };
}
