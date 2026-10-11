import { Router } from "express";
import { requireAuth } from "../middlewares/auth.js";
import { commit } from "../controllers/scans.controller.js";

const r = Router();
r.use(requireAuth);
r.post("/:id/commit", commit);
export default r;
