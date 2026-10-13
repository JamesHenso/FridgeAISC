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
  const ings = [
    { id: "egg", name: "Trứng" },
    { id: "salt", name: "Muối" },
    { id: "milk", name: "Sữa" },
  ];
  it("counts known-but-absent as missing even with null qty", () => {
    const rows = [
      { recipeId: "a", ingredientId: "egg", qty_base: 100 },
      { recipeId: "a", ingredientId: "salt", qty_base: null },
      { recipeId: "b", ingredientId: "egg", qty_base: 100 },
      { recipeId: "b", ingredientId: "milk", qty_base: 500 },
    ];
    const got = rankByMissing(rows, [{ ingredientId: "egg", qty_base: 200 }], ings);
    assert.equal(got[0].recipeId, "a");
    assert.equal(got[0].missing, 1); // salt known but absent
    assert.equal(got[0].matched, 1);
    assert.deepEqual(got[0].missingIngredients, [{ ingredientId: "salt", name: "Muối" }]);
    assert.deepEqual(got[0].matchedIngredients, [{ ingredientId: "egg", name: "Trứng" }]);
    assert.equal(got[1].missing, 1); // milk known but absent (qty compare not reached)
  });
  it("skips null-ingredient rows, qty shortfall counts", () => {
    const rows = [
      { recipeId: "a", ingredientId: null, qty_base: null },
      { recipeId: "a", ingredientId: "egg", qty_base: 500 },
    ];
    const got = rankByMissing(rows, [{ ingredientId: "egg", qty_base: 100 }], ings);
    assert.equal(got[0].missing, 1);
    assert.equal(got[0].enough, false);
  });
  it("excludes recipes with zero fridge overlap", () => {
    const rows = [
      { recipeId: "a", ingredientId: "egg", qty_base: null },
      { recipeId: "far", ingredientId: "salt", qty_base: null },
    ];
    const got = rankByMissing(rows, [{ ingredientId: "egg", qty_base: 200 }], ings);
    assert.deepEqual(got.map((r) => r.recipeId), ["a"]);
  });
});

describe("matchRecipes", () => {
  it("lists only overlapping recipes, most-overlap first, with missing names", async () => {
    const rows = [
      { recipeId: "vit", ingredientId: "duck", qty_base: null, base_unit: null },
      { recipeId: "vit", ingredientId: "salt", qty_base: null, base_unit: null },
      { recipeId: "trung", ingredientId: "egg", qty_base: null, base_unit: null },
      { recipeId: "trung-ca", ingredientId: "egg", qty_base: null, base_unit: null },
      { recipeId: "trung-ca", ingredientId: "fish", qty_base: null, base_unit: null },
    ];
    const ings = [
      { id: "egg", name: "Trứng" },
      { id: "fish", name: "Cá" },
      { id: "duck", name: "Vịt" },
      { id: "salt", name: "Muối" },
    ];
    const got = await matchRecipes(
      [{ name: "Trứng", qty_base: 5 }, { name: "Cá", qty_base: 2 }],
      {},
      async () => rows,
      async () => ings,
    );
    assert.deepEqual(got.map((r) => r.recipeId), ["trung-ca", "trung"]); // vit excluded: zero overlap
    assert.equal(got[0].matched, 2);
    assert.equal(got[0].missing, 0);
    assert.ok(!("recipeName" in got[0])); // card lay ten tu fetchRecipeCards, rank khong fetch ten nua
    assert.deepEqual(got[0].matchedIngredients, [
      { ingredientId: "egg", name: "Trứng" },
      { ingredientId: "fish", name: "Cá" },
    ]);
    assert.deepEqual(got[0].missingIngredients, []);
    assert.equal(got[1].matched, 1);
  });
  it("slices top N after sort when limit given", async () => {
    const rows = [
      { recipeId: "a", ingredientId: "egg", qty_base: null, base_unit: null },
      { recipeId: "b", ingredientId: "egg", qty_base: null, base_unit: null },
      { recipeId: "b", ingredientId: "fish", qty_base: null, base_unit: null },
      { recipeId: "c", ingredientId: "fish", qty_base: null, base_unit: null },
    ];
    const ings = [
      { id: "egg", name: "Trứng" },
      { id: "fish", name: "Cá" },
    ];
    const got = await matchRecipes(
      [{ name: "Trứng" }, { name: "Cá" }],
      { limit: 2 },
      async () => rows,
      async () => ings,
    );
    assert.deepEqual(got.map((r) => r.recipeId), ["b", "a"]);
  });
  it("ignores requireAll (removed): returns overlap-ranked like default", async () => {
    const rows = [{ recipeId: "a", ingredientId: "egg", qty_base: null, base_unit: null }];
    const got = await matchRecipes(
      [{ name: "Trứng" }],
      { requireAll: true } as never,
      async () => rows,
      async () => [{ id: "egg", name: "Trứng" }],
    );
    assert.deepEqual(got.map((r) => r.recipeId), ["a"]);
  });
});
