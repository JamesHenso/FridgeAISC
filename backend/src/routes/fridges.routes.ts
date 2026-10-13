import { Router } from "express";
import { requireAuth } from "../middlewares/auth.js";
import { foods, join, members, myFridges, patchFood, removeFood, rename, stats } from "../controllers/fridges.controller.js";

const r = Router();
r.use(requireAuth);
r.get("/", myFridges);
r.post("/join", join);
r.get("/:id/members", members);
r.get("/:id/foods", foods);
r.get("/:id/stats", stats);
r.patch("/:id", rename);
r.patch("/foods/:id", patchFood);
r.delete("/foods/:id", removeFood);
export default r;
