import { Router } from "express";
import { requireAuth } from "../middlewares/auth.js";
import { like, myFavorites, unlike } from "../controllers/favorites.controller.js";

const r = Router();
r.use(requireAuth);
r.get("/", myFavorites);
r.post("/", like);
r.delete("/:recipeId", unlike);
export default r;
