import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { deleteFood, joinFridgeByCode, listFoods, listFridges, listMembers, renameFridge, updateFood } from "../src/repositories/fridge.repo.js";

// ponytail: same inline-chain fake style as scans.test.ts updateScanItem block
function chain(data: unknown = []) {
  const calls: string[] = [];
  const b: any = {
    calls,
    select() { calls.push("select"); return b; },
    eq(k: string, v: unknown) { calls.push(`eq:${k}=${v}`); return b; },
    ilike(k: string, v: unknown) { calls.push(`ilike:${k}=${v}`); return b; },
    order() { calls.push("order"); return b; },
    then(res: any) { return Promise.resolve({ data, error: null, count: null }).then(res); },
  };
  return b;
}

function updateDb(count: number, saved: { patch?: unknown }) {
  return {
    from: () => ({
      update: (p: unknown) => {
        saved.patch = p;
        return { eq: async () => ({ error: null, count }) };
      },
      delete: (_o: unknown) => ({ eq: async () => ({ error: null, count }) }),
    }),
  };
}

describe("listFoods search", () => {
  it("filters name by ilike when q given", async () => {
    const b = chain([]);
    await listFoods("f1", undefined, "trung", { from: () => b } as never);
    assert.ok(b.calls.includes("ilike:name=%trung%"));
  });
  it("skips ilike when q empty, keeps location filter", async () => {
    const b = chain([]);
    await listFoods("f1", "freezer", undefined, { from: () => b } as never);
    assert.ok(!b.calls.some((c: string) => c.startsWith("ilike")));
    assert.ok(b.calls.includes("eq:location=freezer"));
  });
});

describe("updateFood", () => {
  it("fills qty_base via toBase when qty/unit change", async () => {
    const saved: { patch?: unknown } = {};
    await updateFood("id1", { qty_display: 2, unit_display: "kg" }, updateDb(1, saved) as never);
    assert.deepEqual(saved.patch, { qty_display: 2, unit_display: "kg", qty_base: 2000, base_unit: "g" });
  });
  it("keeps explicit qty_base, rejects bad location, 404 on missing row", async () => {
    const saved: { patch?: unknown } = {};
    await updateFood("id1", { qty_base: 5, base_unit: "g" }, updateDb(1, saved) as never);
    assert.deepEqual(saved.patch, { qty_base: 5, base_unit: "g" });
    await assert.rejects(updateFood("id1", { location: "pantry" }, updateDb(1, saved) as never), /invalid location/);
    await assert.rejects(updateFood("x", { name: "Y" }, updateDb(0, saved) as never), /not found/);
  });
});

describe("deleteFood", () => {
  it("404 on missing row", async () => {
    const saved: { patch?: unknown } = {};
    await assert.rejects(deleteFood("x", updateDb(0, saved) as never), /not found/);
  });
});

describe("renameFridge", () => {
  it("rejects empty name, 404 on missing row", async () => {
    const saved: { patch?: unknown } = {};
    await assert.rejects(renameFridge("f1", "  ", updateDb(1, saved) as never), /name required/);
    await assert.rejects(renameFridge("x", "Tu moi", updateDb(0, saved) as never), /not found/);
  });
});

describe("listFridges", () => {
  function db() {
    return {
      from: (t: string) => {
        if (t === "fridge_members")
          return { select: () => ({ eq: async () => ({ data: [{ fridge_id: "f1", role: "owner" }, { fridge_id: "f2", role: "member" }], error: null }) }) };
        return {
          select: () => ({
            eq: async () => ({ data: [{ id: "f3", name: "Tu rieng", invite_code: "RIENG" }], error: null }),
            in: async () => ({
              data: [
                { id: "f1", name: "Tu nha", invite_code: "NHA123" },
                { id: "f2", name: "Tu ngoai", invite_code: "NGOAI" },
                { id: "f3", name: "Tu rieng", invite_code: "RIENG" },
              ],
              error: null,
            }),
          }),
        };
      },
    };
  }
  it("merges owned + member fridges with roles, sorted by name", async () => {
    const got = await listFridges("u1", db() as never);
    assert.deepEqual(got, [
      { id: "f2", name: "Tu ngoai", invite_code: "NGOAI", role: "member" },
      { id: "f1", name: "Tu nha", invite_code: "NHA123", role: "owner" },
      { id: "f3", name: "Tu rieng", invite_code: "RIENG", role: "owner" },
    ]);
  });
});

describe("joinFridgeByCode", () => {
  function db(found: unknown, insertErr: unknown = null) {
    return {
      from: (t: string) => {
        if (t === "fridges")
          return { select: () => ({ eq: () => ({ maybeSingle: async () => ({ data: found, error: null }) }) }) };
        return { insert: async () => ({ error: insertErr }) };
      },
    };
  }
  it("rejects empty code, 404 on unknown code", async () => {
    await assert.rejects(joinFridgeByCode("u1", "  ", db(null) as never), /code required/);
    await assert.rejects(joinFridgeByCode("u1", "SAI123", db(null) as never), /invalid code/);
  });
  it("adds member, idempotent when already joined", async () => {
    assert.deepEqual(await joinFridgeByCode("u1", "NHA123", db({ id: "f1" }) as never), { fridgeId: "f1", already: false });
    assert.deepEqual(await joinFridgeByCode("u1", "NHA123", db({ id: "f1" }, { code: "23505" }) as never), { fridgeId: "f1", already: true });
  });
});

describe("listMembers", () => {
  it("maps roles + profiles, empty when no members", async () => {
    const full = {
      from: (t: string) => {
        if (t === "fridge_members")
          return { select: () => ({ eq: async () => ({ data: [{ user_id: "u1", role: "owner" }, { user_id: "u2", role: "member" }], error: null }) }) };
        return { select: () => ({ in: async () => ({ data: [{ id: "u1", name: "Chu", email: "chu@x.vn" }, { id: "u2", name: "Vo", email: "vo@x.vn" }], error: null }) }) };
      },
    };
    assert.deepEqual(await listMembers("f1", full as never), [
      { user_id: "u1", role: "owner", name: "Chu", email: "chu@x.vn" },
      { user_id: "u2", role: "member", name: "Vo", email: "vo@x.vn" },
    ]);
    const empty = { from: () => ({ select: () => ({ eq: async () => ({ data: [], error: null }) }) }) };
    assert.deepEqual(await listMembers("f9", empty as never), []);
  });
});
