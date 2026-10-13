import type { Request, Response } from "express";
import { matchRecipes } from "../services/match.service.js";
import { attachDetails, getRecipe, getReels } from "../services/recipes.service.js";
import { fetchIngredients, fetchRecipeCards, searchRecipes } from "../repositories/recipes.repo.js";
import { fetchFoods } from "../repositories/fridge.repo.js";

function sendErr(res: Response, e: unknown) {
  const err = e as Error & { status?: number };
  res.status(err.status ?? 500).json({ error: err.message });
}

function parseLimit(v: unknown, def: number): number | undefined {
  if (v === undefined) return def;
  const n = Number(v);
  if (!Number.isFinite(n) || n <= 0) return undefined;
  return Math.min(n, 50);
}

// Top N card chung (khong kem matched/missing); tap vao moi goi detail.
export async function match(req: Request, res: Response) {
  try {
    const fridgeId = String(req.query.fridge_id ?? "");
    if (!fridgeId) return res.status(400).json({ error: "fridge_id required" });
    const limit = parseLimit(req.query.limit, 10);
    if (limit === undefined) return res.status(400).json({ error: "invalid limit" });
    const ranked = await matchRecipes(await fetchFoods(fridgeId), { limit });
    res.json(await attachDetails(await fetchRecipeCards(ranked.map((r) => r.recipeId))));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function search(req: Request, res: Response) {
  try {
    const category = typeof req.query.category === "string" ? req.query.category : undefined;
    const method = typeof req.query.method === "string" ? req.query.method : undefined;
    const name = typeof req.query.name === "string" ? req.query.name : undefined;
    const limit = parseLimit(req.query.limit, 10);
    if (limit === undefined) return res.status(400).json({ error: "invalid limit" });
    res.json(await attachDetails(await searchRecipes({ category, method, name, limit })));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function detail(req: Request, res: Response) {
  try {
    const fridgeId = typeof req.query.fridge_id === "string" && req.query.fridge_id ? req.query.fridge_id : undefined;
    const fridge = fridgeId
      ? { items: await fetchFoods(fridgeId), ings: await fetchIngredients() }
      : undefined;
    res.json(await getRecipe(req.params.id, undefined, undefined, undefined, undefined, fridge));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function reels(req: Request, res: Response) {
  try {
    const limit = parseLimit(req.query.limit, 10);
    if (limit === undefined) return res.status(400).json({ error: "invalid limit" });
    res.json(await getReels(limit));
  } catch (e) {
    sendErr(res, e);
  }
}
