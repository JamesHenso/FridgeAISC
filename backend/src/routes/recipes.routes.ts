import { Router } from "express";
import { requireAuth } from "../middlewares/auth.js";
import { detail, match, methods, reels, search } from "../controllers/recipes.controller.js";

const r = Router();
r.use(requireAuth);
r.get("/methods", methods);
r.get("/match", match);
r.get("/search", search);
r.get("/reels", reels);
r.get("/:id", detail);
export default r;
