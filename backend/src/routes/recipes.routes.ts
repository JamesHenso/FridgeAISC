import { Router } from "express";
import { requireAuth } from "../middlewares/auth.js";
import { detail, match, reels, search } from "../controllers/recipes.controller.js";

const r = Router();
r.use(requireAuth);
r.get("/match", match);
r.get("/search", search);
r.get("/reels", reels);
r.get("/:id", detail);
export default r;
