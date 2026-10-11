import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { commitScan } from "../src/services/scans.service.js";

const items = [{ name: "Trứng", qty_display: 5, unit_display: "quả", qty_base: 5, base_unit: "pcs", expiry_date: null, location: "fridge" }];

function deps(over: Record<string, unknown> = {}) {
  const calls: string[] = [];
  return {
    calls,
    getScan: async () => ({ id: "s1", fridge_id: "f1", status: "draft" }),
    fetchItems: async () => { calls.push("fetch"); return items; },
    insertFoods: async () => { calls.push("insert"); return 1; },
    markCommitted: async () => { calls.push("mark"); },
    ...over,
  } as never;
}

describe("commitScan", () => {
  it("inserts then marks committed, in order", async () => {
    const d = deps();
    const got = await commitScan("s1", d);
    assert.deepEqual(got, { inserted: 1, already: false });
    assert.deepEqual(d.calls, ["fetch", "insert", "mark"]);
  });
  it("404 on unknown scan, skips committed", async () => {
    await assert.rejects(commitScan("x", deps({ getScan: async () => null })), /scan not found/);
    const d = deps({ getScan: async () => ({ id: "s1", fridge_id: "f1", status: "committed" }) });
    assert.deepEqual(await commitScan("s1", d), { inserted: 0, already: true });
    assert.deepEqual(d.calls, []);
  });
});
