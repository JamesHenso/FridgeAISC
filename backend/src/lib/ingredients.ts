export interface IngredientEntry {
  id: string;
  name: string;
}

// ponytail: exact lowercase match only; unaccent/fuzzy when scan OCR typos appear
export function normalizeName(name: string | null | undefined): string {
  return (name ?? "").trim().toLowerCase();
}

export function resolveIngredientId(
  raw: string | null | undefined,
  ingredients: IngredientEntry[],
): string | null {
  const norm = normalizeName(raw);
  if (!norm) return null;
  const hit = ingredients.find((i) => normalizeName(i.name) === norm);
  return hit ? hit.id : null;
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const ings = [
    { id: "1", name: "Trứng" },
    { id: "2", name: "Dầu điều" },
  ];
  console.assert(resolveIngredientId("trứng", ings) === "1", "lowercase");
  console.assert(resolveIngredientId("  Trứng ", ings) === "1", "trim");
  console.assert(resolveIngredientId("DẦU ĐIỀU", ings) === "2", "upper");
  console.assert(resolveIngredientId("Vịt", ings) === null, "unknown");
  console.assert(resolveIngredientId(null, ings) === null, "null");
  console.log("resolve self-check ok");
}
