export function* containingAll(
  inputs: string[],
  rows: Array<{ recipe_id: string; ingredient_id: string }>,
): Generator<string> {
  if (inputs.length === 0) return;
  const need = new Set(inputs);
  const byRecipe = new Map<string, Set<string>>();
  for (const r of rows) {
    let s = byRecipe.get(r.recipe_id);
    if (!s) byRecipe.set(r.recipe_id, (s = new Set()));
    s.add(r.ingredient_id);
  }
  for (const [id, have] of byRecipe)
    if ([...need].every((i) => have.has(i))) yield id;
}
