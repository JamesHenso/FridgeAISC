import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { containingAll } from "../src/lib/matchAll.js";

describe("containingAll", () => {
  it("keeps recipes having every input ingredient", () => {
    const rows = [
      { recipe_id: "a", ingredient_id: "egg" },
      { recipe_id: "a", ingredient_id: "tomato" },
      { recipe_id: "b", ingredient_id: "egg" },
    ];
    assert.deepEqual([...containingAll(["egg", "tomato"], rows)], ["a"]);
  });

  it("returns empty when input list is empty", () => {
    assert.deepEqual(
      [...containingAll([], [{ recipe_id: "a", ingredient_id: "egg" }])],
      [],
    );
  });
});
