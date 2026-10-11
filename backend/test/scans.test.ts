import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { commitScan } from "../src/services/scans.service.js";

const items = [{ name: "Vịt", qty_display: 1, unit_display: null, qty_base: null, base_unit: null, expiry_date: null, location: "fridge" }];
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
});
