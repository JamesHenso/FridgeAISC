import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { toBase } from "../src/lib/units.js";
import { rankByMissing } from "../src/services/match.service.js";

describe("toBase", () => {
  it("trims dirty quantity + presence-only on null", () => {
    assert.deepEqual(toBase("3    ", "củ"), { qty_base: 3, base_unit: "pcs", is_estimate: true });
    assert.deepEqual(toBase(null, null), { qty_base: null, base_unit: null, is_estimate: true });
  });
  it("converts kg->g and Muong canh shorthand->ml", () => {
    assert.deepEqual(toBase(2, "kg"), { qty_base: 2000, base_unit: "g", is_estimate: false });
    assert.deepEqual(toBase("2", "1M"), { qty_base: 30, base_unit: "ml", is_estimate: false });
  });
});

describe("rankByMissing", () => {
  it("ranks fewest-missing first, qty compare when known", () => {
    const rows = [
      { recipeId: "a", ingredientId: "egg", qty_base: 100 },
      { recipeId: "a", ingredientId: "salt", qty_base: null },
      { recipeId: "b", ingredientId: "egg", qty_base: 100 },
      { recipeId: "b", ingredientId: "milk", qty_base: 500 },
    ];
    const got = rankByMissing(rows, [{ ingredientId: "egg", qty_base: 200 }]);
    assert.equal(got[0].recipeId, "a");
    assert.equal(got[0].missing, 0); // salt presence-only → not counted
    assert.equal(got[1].missing, 1);
  });
});
