import { Router } from "express";
import { requireAuth } from "../middlewares/auth.js";
import { match } from "../controllers/recipes.controller.js";

const r = Router();
r.use(requireAuth);
r.get("/match", match);
export default r;
