import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { commitScan, createScanWithItems, updateScanItems, updateScanLocations } from "../src/services/scans.service.js";
import { updateScanItem } from "../src/repositories/scans.repo.js";
import { inventoryStats } from "../src/repositories/fridge.repo.js";

const items = [{ name: "Vịt", qty_display: 1, unit_display: null, qty_base: null, base_unit: null, purchased_at: null, expiry_date: null, location: "fridge" }];
const ings = [{ id: "duck-id", name: "Vịt" }];

function deps(over: Record<string, unknown> = {}) {
  const calls: string[] = [];
  let saved: unknown = null;
  return {
    calls,
    saved: () => saved,
    getScan: async () => ({ id: "s1", fridge_id: "f1", status: "draft" }),
    fetchItems: async () => { calls.push("fetch"); return items; },
    fetchIngredients: async () => ings,
    insertFoods: async (_f: string, rows: unknown) => { calls.push("insert"); saved = rows; return 1; },
    markCommitted: async () => { calls.push("mark"); },
    createScan: async () => "s1",
    addItems: async (_s: string, rows: unknown) => { const created = (rows as unknown[]).map((r, n) => ({ id: `i${n}`, name: (r as { name: string }).name })); saved = rows; return created; },
    updateItem: async () => { calls.push("update"); },
    ...over,
  } as never;
}

describe("commitScan", () => {
  it("resolves ingredient_id at commit, then inserts + marks, in order", async () => {
    const d = deps();
    const got = await commitScan("s1", d);
    assert.deepEqual(got, { inserted: 1, already: false });
    assert.deepEqual(d.calls, ["fetch", "insert", "mark"]);
    assert.deepEqual((d.saved() as Array<{ ingredient_id: string }>).map((r) => r.ingredient_id), ["duck-id"]);
  });
  it("keeps pre-mapped id from OCR/BM25 step", async () => {
    const d = deps({ fetchItems: async () => [{ ...items[0], ingredient_id: "keep-id" }] });
    await commitScan("s1", d);
    assert.deepEqual((d.saved() as Array<{ ingredient_id: string }>).map((r) => r.ingredient_id), ["keep-id"]);
  });
  it("404 on unknown scan, skips committed", async () => {
    await assert.rejects(commitScan("x", deps({ getScan: async () => null })), /scan not found/);
    const d = deps({ getScan: async () => ({ id: "s1", fridge_id: "f1", status: "committed" }) });
    assert.deepEqual(await commitScan("s1", d), { inserted: 0, already: true });
    assert.deepEqual(d.calls, []);
  });
  it("rejects items missing location", async () => {
    const d = deps({ fetchItems: async () => [{ ...items[0], location: null }] });
    await assert.rejects(commitScan("s1", d), /location required/);
  });
  it("fills qty_base via toBase + keeps location/purchased_at", async () => {
    const d = deps({ fetchItems: async () => [{ name: "Thịt", ingredient_id: "m1", qty_display: 2, unit_display: "kg", qty_base: null, base_unit: null, purchased_at: "2026-10-01", expiry_date: "2026-10-20", location: "freezer" }] });
    await commitScan("s1", d);
    assert.deepEqual(d.saved(), [{ name: "Thịt", ingredient_id: "m1", qty_display: 2, unit_display: "kg", qty_base: 2000, base_unit: "g", purchased_at: "2026-10-01", expiry_date: "2026-10-20", location: "freezer" }]);
  });
});

describe("manual flow", () => {
  it("create requires fridge_id and retains unrecognized OCR items for review", async () => {
    await assert.rejects(createScanWithItems("", [{ name: "X", ingredient_id: "i1" }], deps()), /fridge_id required/);
    const unknown = deps();
    await createScanWithItems("f1", [{ name: "X" }], unknown);
    assert.equal((unknown.saved() as Array<{ ingredient_id: string | null }>)[0].ingredient_id, null);
    const d = deps();
    assert.deepEqual(await createScanWithItems("f1", [{ name: "Vịt", ingredient_id: "duck-id", qty_display: 1 }], d), { scanId: "s1", inserted: 1, items: [{ id: "i0", name: "Vịt" }] });
  });
  it("locate rejects invalid location", async () => {
    await assert.rejects(updateScanLocations("s1", [{ id: "i1", location: "pantry" }], deps()), /invalid location/);
    const d = deps();
    assert.deepEqual(await updateScanLocations("s1", [{ id: "i1", location: "freezer" }], d), { updated: 1 });
    assert.deepEqual(d.calls, ["update"]);
  });
  it("edit items is a no-op when nothing changed", async () => {
    const d = deps();
    assert.deepEqual(await updateScanItems("s1", [], d), { updated: 0 });
    assert.deepEqual(d.calls, []);
  });
});

describe("updateScanItem", () => {
  it("throws 404 when id/scan mismatch (no silent no-op)", async () => {
    const empty = { from: () => ({ update: () => ({ eq: () => ({ eq: async () => ({ error: null, count: 0 }) }) }) }) };
    await assert.rejects(updateScanItem("wrong-id", "s1", { location: "fridge" }, empty), /not found in scan/);
  });
});

describe("inventoryStats", () => {
  it("buckets useToday/useSoon/cleanOut by day", () => {
    const today = new Date("2026-10-12T00:00:00Z");
    const rows = [
      { expiry_date: "2026-10-10" }, // cleanOut
      { expiry_date: "2026-10-12" }, // today
      { expiry_date: "2026-10-13" }, // soon
      { expiry_date: "2026-10-15" }, // soon (3d)
      { expiry_date: "2026-10-20" }, // stock only
      { expiry_date: null }, // stock only
    ];
    assert.deepEqual(inventoryStats(rows, today), { inStock: 5, useToday: 1, useSoon: 2, cleanOut: 1 });
  });
});
