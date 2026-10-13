import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { likeRecipe, unlikeRecipe } from "../src/services/favorites.service.js";
import { addFavorite, bumpLikeCount, listFavoriteRecipeIds, removeFavorite } from "../src/repositories/favorites.repo.js";

describe("likeRecipe", () => {
  it("inserts + bumps count on first like", async () => {
    const bumped: unknown[] = [];
    const got = await likeRecipe("u1", "r1", async () => false, async (...a: unknown[]) => { bumped.push(a); });
    assert.deepEqual(got, { liked: true, already: false });
    assert.deepEqual(bumped, [["r1", 1]]);
  });
  it("skips bump when already liked", async () => {
    let bumped = 0;
    const got = await likeRecipe("u1", "r1", async () => true, async () => { bumped++; });
    assert.deepEqual(got, { liked: true, already: true });
    assert.equal(bumped, 0);
  });
});

describe("unlikeRecipe", () => {
  it("removes + decrements count", async () => {
    const bumped: unknown[] = [];
    const got = await unlikeRecipe("u1", "r1", async () => true, async (...a: unknown[]) => { bumped.push(a); });
    assert.deepEqual(got, { unliked: true, removed: true });
    assert.deepEqual(bumped, [["r1", -1]]);
  });
  it("skips decrement when not liked", async () => {
    let bumped = 0;
    const got = await unlikeRecipe("u1", "r1", async () => false, async () => { bumped++; });
    assert.deepEqual(got, { unliked: true, removed: false });
    assert.equal(bumped, 0);
  });
});

describe("favorites repo", () => {
  it("addFavorite maps duplicate PK to already=true, throws other errors", async () => {
    const dup = { from: () => ({ insert: async () => ({ error: { code: "23505" } }) }) };
    assert.equal(await addFavorite("u1", "r1", dup as never), true);
    const boom = { from: () => ({ insert: async () => ({ error: { code: "500", message: "x" } }) }) };
    await assert.rejects(addFavorite("u1", "r1", boom as never));
    const ok = { from: () => ({ insert: async () => ({ error: null }) }) };
    assert.equal(await addFavorite("u1", "r1", ok as never), false);
  });
  it("removeFavorite returns whether a row was removed", async () => {
    const one = { from: () => ({ delete: () => ({ eq: () => ({ eq: async () => ({ error: null, count: 1 }) }) }) }) };
    assert.equal(await removeFavorite("u1", "r1", one as never), true);
    const zero = { from: () => ({ delete: () => ({ eq: () => ({ eq: async () => ({ error: null, count: 0 }) }) }) }) };
    assert.equal(await removeFavorite("u1", "r1", zero as never), false);
  });
  it("listFavoriteRecipeIds returns newest-first ids", async () => {
    const calls: string[] = [];
    const db = {
      from: (t: string) => {
        calls.push(t);
        return {
          select: () => ({
            eq: (k: string, v: unknown) => {
              calls.push(`eq:${k}=${v}`);
              return { order: async () => ({ data: [{ recipe_id: "r2" }, { recipe_id: "r1" }], error: null }) };
            },
          }),
        };
      },
    };
    assert.deepEqual(await listFavoriteRecipeIds("u1", db as never), ["r2", "r1"]);
    assert.ok(calls.includes("eq:user_id=u1"));
  });
  it("bumpLikeCount calls rpc with id + delta", async () => {
    let saved: unknown = null;
    const db = { rpc: async (fn: string, args: unknown) => { saved = [fn, args]; return { error: null }; } };
    await bumpLikeCount("r1", -1, db as never);
    assert.deepEqual(saved, ["bump_like_count", { rid: "r1", delta: -1 }]);
  });
});
