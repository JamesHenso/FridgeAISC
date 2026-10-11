import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { toBase } from "../src/lib/units.js";
import { resolveIngredientId } from "../src/lib/ingredients.js";
import { rankByMissing, matchRecipes } from "../src/services/match.service.js";

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

describe("resolveIngredientId", () => {
  const ings = [{ id: "egg", name: "Trứng" }];
  it("matches case-insensitive + trim, null when unknown", () => {
    assert.equal(resolveIngredientId("trứng", ings), "egg");
    assert.equal(resolveIngredientId("  Trứng ", ings), "egg");
    assert.equal(resolveIngredientId("Vịt", ings), null);
  });
});

describe("rankByMissing", () => {
  it("counts known-but-absent as missing even with null qty", () => {
    const rows = [
      { recipeId: "a", ingredientId: "egg", qty_base: 100 },
      { recipeId: "a", ingredientId: "salt", qty_base: null },
      { recipeId: "b", ingredientId: "egg", qty_base: 100 },
      { recipeId: "b", ingredientId: "milk", qty_base: 500 },
    ];
    const got = rankByMissing(rows, [{ ingredientId: "egg", qty_base: 200 }]);
    assert.equal(got[0].recipeId, "a");
    assert.equal(got[0].missing, 1); // salt known but absent
    assert.equal(got[1].missing, 1); // milk known but absent (qty compare not reached)
  });
  it("skips null-ingredient rows, qty shortfall counts", () => {
    const rows = [
      { recipeId: "a", ingredientId: null, qty_base: null },
      { recipeId: "a", ingredientId: "egg", qty_base: 500 },
    ];
    const got = rankByMissing(rows, [{ ingredientId: "egg", qty_base: 100 }]);
    assert.equal(got[0].missing, 1);
    assert.equal(got[0].enough, false);
  });
});

describe("matchRecipes", () => {
  it("resolves raw names then ranks most-overlap first", async () => {
    const rows = [
      { recipeId: "vit", ingredientId: "duck", qty_base: null, base_unit: null },
      { recipeId: "vit", ingredientId: "salt", qty_base: null, base_unit: null },
      { recipeId: "trung", ingredientId: "egg", qty_base: null, base_unit: null },
    ];
    const ings = [{ id: "egg", name: "Trứng" }, { id: "fish", name: "Cá" }];
    const got = await matchRecipes(
      [{ name: "Trứng", qty_base: 5 }, { name: "Cá", qty_base: 2 }],
      {},
      async () => rows,
      async () => ings,
    );
    assert.equal(got[0].recipeId, "trung");
    assert.equal(got[0].missing, 0);
    assert.equal(got[1].missing, 2);
  });
});
