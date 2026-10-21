import type { Request, Response } from "express";
import { createFridge, deleteFood, joinFridgeByCode, listFoods, listFridges, listMembers, inventoryStats, renameFridge, updateFood } from "../repositories/fridge.repo.js";

function sendErr(res: Response, e: unknown) {
  const err = e as Error & { status?: number };
  res.status(err.status ?? 500).json({ error: err.message });
}

export async function myFridges(req: Request, res: Response) {
  try {
    if (!req.user?.id) return res.status(401).json({ error: "missing user" });
    res.json(await listFridges(req.user.id));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function create(req: Request, res: Response) {
  try {
    if (!req.user?.id) return res.status(401).json({ error: "missing user" });
    res.status(201).json(await createFridge(req.user.id, String(req.body?.name ?? "")));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function foods(req: Request, res: Response) {
  try {
    const loc = typeof req.query.location === "string" ? req.query.location : undefined;
    if (loc && loc !== "fridge" && loc !== "freezer") return res.status(400).json({ error: "invalid location" });
    const q = typeof req.query.q === "string" ? req.query.q : undefined;
    res.json(await listFoods(req.params.id, loc, q));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function stats(req: Request, res: Response) {
  try {
    res.json(inventoryStats(await listFoods(req.params.id)));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function patchFood(req: Request, res: Response) {
  try {
    await updateFood(req.params.id, req.body ?? {});
    res.json({ updated: 1 });
  } catch (e) {
    sendErr(res, e);
  }
}

export async function removeFood(req: Request, res: Response) {
  try {
    await deleteFood(req.params.id);
    res.json({ deleted: 1 });
  } catch (e) {
    sendErr(res, e);
  }
}

export async function rename(req: Request, res: Response) {
  try {
    await renameFridge(req.params.id, String(req.body?.name ?? ""));
    res.json({ updated: 1 });
  } catch (e) {
    sendErr(res, e);
  }
}

async function requireMember(userId: string | undefined, fridgeId: string, res: Response): Promise<boolean> {
  if (!userId) {
    res.status(401).json({ error: "missing user" });
    return false;
  }
  const mine = await listFridges(userId);
  if (!mine.some((f) => f.id === fridgeId)) {
    res.status(403).json({ error: "not a member" });
    return false;
  }
  return true;
}

export async function members(req: Request, res: Response) {
  try {
    if (!(await requireMember(req.user?.id, req.params.id, res))) return;
    res.json(await listMembers(req.params.id));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function join(req: Request, res: Response) {
  try {
    if (!req.user?.id) return res.status(401).json({ error: "missing user" });
    res.json(await joinFridgeByCode(req.user.id, String(req.body?.code ?? "")));
  } catch (e) {
    sendErr(res, e);
  }
}
