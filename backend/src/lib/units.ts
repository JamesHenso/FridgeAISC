// ponytail: unit map covers crawl/QR shorthands only (M=muỗng canh 15ml, m=muỗng cf 5ml); extend when new units appear in data
const TO_BASE: Record<string, { unit: string; factor: number }> = {
  g: { unit: "g", factor: 1 },
  kg: { unit: "g", factor: 1000 },
  ml: { unit: "ml", factor: 1 },
  l: { unit: "ml", factor: 1000 },
  "muỗng canh": { unit: "ml", factor: 15 },
  "1m": { unit: "ml", factor: 15 },
  "1M": { unit: "ml", factor: 15 },
  "2M": { unit: "ml", factor: 30 },
  "muỗng cf": { unit: "ml", factor: 5 },
  "1m_cf": { unit: "ml", factor: 5 },
  m: { unit: "ml", factor: 5 },
};

export interface ToBaseResult {
  qty_base: number | null;
  base_unit: string | null;
  is_estimate: boolean;
}

export function toBase(
  qty: string | number | null | undefined,
  unit: string | null | undefined,
  pcs_to_gram?: number | null,
): ToBaseResult {
  const q =
    typeof qty === "number"
      ? qty
      : qty == null || String(qty).trim() === ""
        ? NaN
        : Number(String(qty).trim().replace(",", "."));
  const u = unit == null ? "" : String(unit).trim().toLowerCase();
  if (!Number.isFinite(q)) return { qty_base: null, base_unit: null, is_estimate: true };
  if (!u || ["củ", "quả", "trái", "miếng", "pcs", "cái"].includes(u))
    return {
      qty_base: pcs_to_gram ? q * pcs_to_gram : q,
      base_unit: pcs_to_gram ? "g" : "pcs",
      is_estimate: !pcs_to_gram,
    };
  const m = TO_BASE[u];
  if (!m) return { qty_base: null, base_unit: null, is_estimate: true };
  return { qty_base: q * m.factor, base_unit: m.unit, is_estimate: false };
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const cases: Array<[unknown, unknown, unknown, ToBaseResult]> = [
    ["3    ", "củ", undefined, { qty_base: 3, base_unit: "pcs", is_estimate: true }],
    [null, null, undefined, { qty_base: null, base_unit: null, is_estimate: true }],
    ["", "g", undefined, { qty_base: null, base_unit: null, is_estimate: true }],
    [2, "kg", undefined, { qty_base: 2000, base_unit: "g", is_estimate: false }],
    ["1M", null, undefined, { qty_base: null, base_unit: null, is_estimate: true }],
    ["2", "1M", undefined, { qty_base: 30, base_unit: "ml", is_estimate: false }],
    [1, "củ", 100, { qty_base: 100, base_unit: "g", is_estimate: false }],
  ];
  for (const [q, u, p, exp] of cases) {
    const got = toBase(q as never, u as never, p as never);
    console.assert(JSON.stringify(got) === JSON.stringify(exp), `toBase(${q},${u})`, got);
  }
  console.log("toBase self-check ok");
}
