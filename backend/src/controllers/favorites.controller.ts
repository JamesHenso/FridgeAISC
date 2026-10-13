import type { Request, Response } from "express";
import { likeRecipe, unlikeRecipe } from "../services/favorites.service.js";
import { listFavoriteRecipeIds } from "../repositories/favorites.repo.js";
import { attachDetails } from "../services/recipes.service.js";
import { fetchRecipeCards } from "../repositories/recipes.repo.js";

function sendErr(res: Response, e: unknown) {
  const err = e as Error & { status?: number };
  res.status(err.status ?? 500).json({ error: err.message });
}

function userId(req: Request, res: Response): string | null {
  if (!req.user?.id) {
    res.status(401).json({ error: "missing user" });
    return null;
  }
  return req.user.id;
}

export async function myFavorites(req: Request, res: Response) {
  try {
    const uid = userId(req, res);
    if (!uid) return;
    res.json(await attachDetails(await fetchRecipeCards(await listFavoriteRecipeIds(uid))));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function like(req: Request, res: Response) {
  try {
    const uid = userId(req, res);
    if (!uid) return;
    const recipeId = String(req.body?.recipe_id ?? "");
    if (!recipeId) return res.status(400).json({ error: "recipe_id required" });
    res.json(await likeRecipe(uid, recipeId));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function unlike(req: Request, res: Response) {
  try {
    const uid = userId(req, res);
    if (!uid) return;
    res.json(await unlikeRecipe(uid, req.params.recipeId));
  } catch (e) {
    sendErr(res, e);
  }
}
