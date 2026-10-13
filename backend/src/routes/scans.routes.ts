import { Router } from "express";
import { requireAuth } from "../middlewares/auth.js";
import { commit, create, editItems, locate } from "../controllers/scans.controller.js";

const r = Router();
r.use(requireAuth);
r.post("/", create);
r.put("/:id/items", editItems);
r.put("/:id/locations", locate);
r.post("/:id/commit", commit);
export default r;
