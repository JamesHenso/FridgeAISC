import "dotenv/config";
import express from "express";
import { fileURLToPath } from "node:url";
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
// Bundled public ingredient icons, independent of the process working directory.
app.use("/assets", express.static(fileURLToPath(new URL("../assets/", import.meta.url)), {
  dotfiles: "deny", index: false, maxAge: "1d",
}));
app.get("/health", (_req, res) => res.json({ ok: true }));
app.use("/scans", scansRoutes);
app.use("/recipes", recipesRoutes);
app.use("/fridges", fridgesRoutes);
app.use("/favorites", favoritesRoutes);

// This module is the backend entry point (used by `tsx watch` and `node dist/server.js`).
// Comparing import.meta.url with process.argv[1] is unreliable on Windows because
// the former is a file URL while the latter is a Windows filesystem path.
const port = Number(process.env.PORT ?? 3000);
app.listen(port, "0.0.0.0", () => {
  console.log(`FridgeWise backend listening on http://0.0.0.0:${port}`);
});
