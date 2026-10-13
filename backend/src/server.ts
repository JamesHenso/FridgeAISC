import "dotenv/config";
import express from "express";
import cors from "cors";
import morgan from "morgan";
import scansRoutes from "./routes/scans.routes.js";
import recipesRoutes from "./routes/recipes.routes.js";
import fridgesRoutes from "./routes/fridges.routes.js";
import favoritesRoutes from "./routes/favorites.routes.js";

export const app = express();
app.use(cors());
app.use(express.json({ limit: "10mb" }));
app.use(morgan("tiny"));
app.get("/health", (_req, res) => res.json({ ok: true }));
app.use("/scans", scansRoutes);
app.use("/recipes", recipesRoutes);
app.use("/fridges", fridgesRoutes);
app.use("/favorites", favoritesRoutes);

if (import.meta.url === `file://${process.argv[1]}`) {
  app.listen(Number(process.env.PORT ?? 3000));
}
