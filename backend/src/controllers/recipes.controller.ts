import type { Request, Response } from "express";
import { matchRecipes } from "../services/match.service.js";
import { fetchFoods } from "../repositories/fridge.repo.js";

export async function match(req: Request, res: Response) {
  try {
    const fridgeId = String(req.query.fridge_id ?? "");
    if (!fridgeId) return res.status(400).json({ error: "fridge_id required" });
    res.json(await matchRecipes(await fetchFoods(fridgeId), {
      requireAll: req.query.requireAll === "1",
    }));
  } catch (e) {
    res.status(500).json({ error: (e as Error).message });
  }
}
