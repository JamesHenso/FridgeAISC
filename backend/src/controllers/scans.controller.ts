import type { Request, Response } from "express";
import { commitScan } from "../services/scans.service.js";

export async function commit(req: Request, res: Response) {
  try {
    res.json(await commitScan(req.params.id));
  } catch (e) {
    const err = e as Error & { status?: number };
    res.status(err.status ?? 500).json({ error: err.message });
  }
}
