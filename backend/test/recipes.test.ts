import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { attachDetails, getRecipe, getReels } from "../src/services/recipes.service.js";
import { fetchReelIds, searchRecipes } from "../src/repositories/recipes.repo.js";

function chain(data: unknown = []) {
  const calls: string[] = [];
  const b: any = {
    calls,
    select() { calls.push("select"); return b; },
    eq(k: string, v: unknown) { calls.push(`eq:${k}=${v}`); return b; },
    ilike(k: string, v: unknown) { calls.push(`ilike:${k}=${v}`); return b; },
    not(k: string, op: string, v: unknown) { calls.push(`not:${k}:${op}:${v}`); return b; },
    order() { calls.push("order"); return b; },
    range() { calls.push("range"); return Promise.resolve({ data, error: null }); },
    limit(n: unknown) { calls.push(`limit:${n}`); return b; },
    in(k: string, v: unknown) { calls.push(`in:${k}=${(v as unknown[]).length}`); return b; },
    then(res: any) { return Promise.resolve({ data, error: null, count: null }).then(res); },
  };
  return b;
}

describe("searchRecipes", () => {
  it("applies category/method filters + default limit 10", async () => {
    const b = chain([]);
    await searchRecipes({ category: "Mon man", method: "Chien" }, { from: () => b } as never);
    assert.ok(b.calls.includes("eq:category=Mon man"));
    assert.ok(b.calls.includes("eq:method=Chien"));
    assert.ok(b.calls.includes("limit:10"));
  });
  it("skips filters when absent", async () => {
    const b = chain([{ id: "r1", name: "Com chien" }]);
    const got = await searchRecipes({}, { from: () => b } as never);
    assert.ok(!b.calls.some((c: string) => c.startsWith("eq:")));
    assert.deepEqual(got, [{ id: "r1", name: "Com chien" }]);
  });
  it("ilike name when given (global name search)", async () => {
    const b = chain([]);
    await searchRecipes({ name: "com" }, { from: () => b } as never);
    assert.ok(b.calls.includes("ilike:name=%com%"));
  });
});

describe("attachDetails", () => {
  const card = { id: "r1", name: "Com chien", description: null, imageUrl: null, category: null, method: null, like_count: 0, video_id: null, video_provider: "youtube" };
  it("merges servings/time/difficulty, null when no detail row", async () => {
    const got = await attachDetails(
      [card, { ...card, id: "r2" }],
      async () => [{ recipeId: "r1", servings: 2, time: 30, difficulty: "De" }] as never,
    );
    assert.deepEqual(got[0], { ...card, servings: 2, time: 30, difficulty: "De" });
    assert.deepEqual(got[1], { ...card, id: "r2", servings: null, time: null, difficulty: null });
  });
});

describe("getRecipe", () => {
  const recipe = { id: "r1", name: "Com chien", description: "Ngon", imageUrl: null, category: "Mon man", method: "Chien", like_count: 0, video_id: null, video_provider: "youtube" };
  const detail = { servings: 2, time: 30, difficulty: "De", preparation: "1M dau", instructions: "Chien len", servingText: null };
  const rows = [{ ingredientId: "e1", name: "Trung", quantity: "3", unit: "qua", qty_base: null, base_unit: null }];
  const deps = [
    async () => recipe,
    async () => detail,
    async () => rows,
    async () => [{ id: "e1", name: "Trứng" }],
  ] as unknown as [never, never, never, never];
  it("assembles info + ingredients (catalog name wins) + steps", async () => {
    const got = await getRecipe("r1", ...deps);
    assert.equal(got.info.name, "Com chien");
    assert.deepEqual(got.ingredients, [
      { ingredientId: "e1", name: "Trứng", quantity: "3", unit: "qua", qty_base: null, base_unit: null },
    ]);
    assert.equal(got.detail?.time, 30);
  });
  it("404 when recipe missing, detail null tolerated", async () => {
    const noRecipe = [async () => null, deps[1], deps[2], deps[3]] as unknown as [never, never, never, never];
    await assert.rejects(getRecipe("x", ...noRecipe), /not found/);
    const noDetail = [deps[0], async () => null, deps[2], deps[3]] as unknown as [never, never, never, never];
    const got = await getRecipe("r1", ...noDetail);
    assert.equal(got.detail, null);
  });
  it("splits have/need when fridge given", async () => {
    const rows2 = [
      ...rows,
      { ingredientId: "e2", name: "Muoi", quantity: null, unit: null, qty_base: null, base_unit: null },
    ];
    const d = [deps[0], deps[1], async () => rows2, async () => [{ id: "e1", name: "Trứng" }, { id: "e2", name: "Muối" }]] as unknown as [never, never, never, never];
    const fridge = { items: [{ ingredientId: "e1", qty_base: 500 }], ings: [{ id: "e1", name: "Trứng" }, { id: "e2", name: "Muối" }] };
    const got = await getRecipe("r1", ...d, fridge as never);
    assert.deepEqual(got.haveNeed?.matched, [{ ingredientId: "e1", name: "Trứng" }]);
    assert.deepEqual(got.haveNeed?.missing, [{ ingredientId: "e2", name: "Muối" }]);
  });
  it("all missing when fridge has zero overlap", async () => {
    const got = await getRecipe("r1", ...deps, { items: [], ings: [{ id: "e1", name: "Trứng" }] } as never);
    assert.deepEqual(got.haveNeed?.matched, []);
    assert.deepEqual(got.haveNeed?.missing, [{ ingredientId: "e1", name: "Trứng" }]);
  });
  it("no haveNeed without fridge", async () => {
    const got = await getRecipe("r1", ...deps);
    assert.equal(got.haveNeed, undefined);
  });
});

describe("reels", () => {
  it("fetchReelIds only takes recipes with video", async () => {
    const b = chain([{ id: "r1" }]);
    const got = await fetchReelIds({ from: () => b } as never);
    assert.ok(b.calls.some((c: string) => c.startsWith("not:video_id")));
    assert.deepEqual(got, ["r1"]);
  });
  it("getReels picks N, returns cards with video + details", async () => {
    const card = (id: string) => ({ id, name: `M${id}`, description: null, imageUrl: null, category: null, method: null, like_count: 0, video_id: `v${id}`, video_provider: "youtube" });
    const got = await getReels(
      2,
      async () => ["r1", "r2", "r3"],
      async (xs: string[]) => xs.map(card) as never,
      async () => [{ recipeId: "r1", servings: 2, time: 15, difficulty: "De" }] as never,
      (xs, n) => xs.slice(0, n),
    );
    assert.equal(got.length, 2);
    assert.deepEqual(got[0], { ...card("r1"), servings: 2, time: 15, difficulty: "De" });
    assert.equal(got[1].video_id, "vr2");
  });
});
