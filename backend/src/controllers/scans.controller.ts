import type { Request, Response } from "express";
import { commitScan, createScanWithItems, updateScanItems, updateScanLocations } from "../services/scans.service.js";

function sendErr(res: Response, e: unknown) {
  const err = e as Error & { status?: number };
  res.status(err.status ?? 500).json({ error: err.message });
}

export async function create(req: Request, res: Response) {
  try {
    res.status(201).json(await createScanWithItems(String(req.body.fridge_id ?? ""), req.body.items ?? [], undefined,
      { source_type: req.body.source_type ?? null, merchant: req.body.merchant ?? null,
        receipt_total_vnd: req.body.receipt_total_vnd ?? null,
        receipt_subtotal_vnd: req.body.receipt_subtotal_vnd ?? null,
        receipt_discount_vnd: req.body.receipt_discount_vnd ?? null,
        receipt_line_total_vnd: req.body.receipt_line_total_vnd ?? null }));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function editItems(req: Request, res: Response) {
  try {
    res.json(await updateScanItems(req.params.id, req.body.items ?? []));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function locate(req: Request, res: Response) {
  try {
    res.json(await updateScanLocations(req.params.id, req.body.items ?? []));
  } catch (e) {
    sendErr(res, e);
  }
}

export async function commit(req: Request, res: Response) {
  try {
    res.json(await commitScan(req.params.id));
  } catch (e) {
    sendErr(res, e);
  }
}
