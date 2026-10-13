import { addFavorite, bumpLikeCount, removeFavorite } from "../repositories/favorites.repo.js";

// Tym = insert favorite + tang like; like lap idempotent (khong +2).
export async function likeRecipe(
  userId: string,
  recipeId: string,
  add = addFavorite,
  bump = bumpLikeCount,
): Promise<{ liked: boolean; already: boolean }> {
  const already = await add(userId, recipeId);
  if (!already) await bump(recipeId, 1);
  return { liked: true, already };
}

export async function unlikeRecipe(
  userId: string,
  recipeId: string,
  remove = removeFavorite,
  bump = bumpLikeCount,
): Promise<{ unliked: boolean; removed: boolean }> {
  const removed = await remove(userId, recipeId);
  if (removed) await bump(recipeId, -1);
  return { unliked: true, removed };
}
