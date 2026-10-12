import csv
import unittest
from pathlib import Path

from evaluate_retrieval import (
    BM25_COVERAGE_RETRIEVER,
    BM25_RETRIEVER,
    CHAR_NGRAM_RETRIEVER,
    FUZZY_WRATIO_RETRIEVER,
    HYBRID_RRF_RETRIEVER,
)
from product_processor import (
    DUAL_BM25_RETRIEVER,
    normalize_for_accentless_lookup,
    normalize_for_accented_lookup,
    process_product,
    process_products,
)


BASE_DIR = Path(__file__).resolve().parents[1]


def load_cases() -> list[dict]:
    with open(
        BASE_DIR / "retrieval_test_cases.csv",
        encoding="utf-8-sig",
        newline="",
    ) as file:
        return list(csv.DictReader(file))


class RetrievalEvaluationTests(unittest.TestCase):
    def accuracy(self, retriever, *, top_k: int) -> float:
        cases = load_cases()
        hits = 0

        for case in cases:
            result_ids = [
                result.document.id
                for result in retriever.search(
                    case["query"],
                    top_k=top_k,
                )
            ]
            if case["expected_id"] in result_ids:
                hits += 1

        return hits / len(cases)

    def test_plain_bm25_baseline_does_not_regress(self):
        self.assertGreaterEqual(
            self.accuracy(BM25_RETRIEVER, top_k=1),
            0.75,
        )

    def test_coverage_reranker_improves_top_one(self):
        plain_accuracy = self.accuracy(BM25_RETRIEVER, top_k=1)
        reranked_accuracy = self.accuracy(
            BM25_COVERAGE_RETRIEVER,
            top_k=1,
        )

        self.assertGreater(reranked_accuracy, plain_accuracy)
        self.assertGreaterEqual(reranked_accuracy, 0.85)

    def test_expected_product_is_in_top_five(self):
        self.assertEqual(
            self.accuracy(BM25_COVERAGE_RETRIEVER, top_k=5),
            1.0,
        )

    def test_dual_index_uses_accents_to_reduce_collisions(self):
        results = DUAL_BM25_RETRIEVER.search(
            "Nước mắm Nam Ngư",
            top_k=2,
        )

        self.assertEqual(
            results[0].document.id,
            "31ad5262-d767-4ba1-b109-ad78c6dbbe80",
        )
        self.assertTrue(
            results[0].score_details["query_has_diacritics"]
        )
        self.assertNotIn(
            "d0a02651-6faf-4035-839e-56c952703a86",
            [result.document.id for result in results],
        )

    def test_dual_index_falls_back_for_unaccented_ocr(self):
        results = DUAL_BM25_RETRIEVER.search(
            "Nuoc mam Nam Ngu",
            top_k=5,
        )

        self.assertIn(
            "31ad5262-d767-4ba1-b109-ad78c6dbbe80",
            [result.document.id for result in results],
        )
        self.assertFalse(
            results[0].score_details["query_has_diacritics"]
        )

    def test_char_ngram_handles_an_ocr_substitution(self):
        results = CHAR_NGRAM_RETRIEVER.search(
            "thlt ba chi heo",
            top_k=5,
        )

        self.assertIn(
            "82de1a34-cd5f-497a-b5c8-95570fe5a6f6",
            [result.document.id for result in results],
        )

    def test_fuzzy_wratio_handles_brand_noise(self):
        results = FUZZY_WRATIO_RETRIEVER.search(
            "bo lat anchor",
            top_k=5,
        )

        self.assertIn(
            "321bb46a-7bfe-40f2-8040-dcc400e3886f",
            [result.document.id for result in results],
        )

    def test_hybrid_rrf_returns_component_ranks(self):
        result = HYBRID_RRF_RETRIEVER.search(
            "nuoc mam nam ngu",
            top_k=1,
        )[0]

        self.assertEqual(
            result.document.id,
            "31ad5262-d767-4ba1-b109-ad78c6dbbe80",
        )
        self.assertIn("rank_bm25", result.score_details)
        self.assertIn("rank_char_ngram", result.score_details)
        self.assertIn("rank_fuzzy", result.score_details)
        self.assertEqual(result.score_details["weight_bm25"], 2.0)
        self.assertEqual(result.score_details["weight_fuzzy"], 0.5)

    def test_new_retrieval_baselines_do_not_regress(self):
        self.assertGreaterEqual(
            self.accuracy(FUZZY_WRATIO_RETRIEVER, top_k=1),
            0.89,
        )
        self.assertEqual(
            self.accuracy(CHAR_NGRAM_RETRIEVER, top_k=5),
            1.0,
        )
        self.assertEqual(
            self.accuracy(HYBRID_RRF_RETRIEVER, top_k=5),
            1.0,
        )


class ProductProcessorTests(unittest.TestCase):
    def test_normalization_keeps_an_accented_and_fallback_form(self):
        self.assertEqual(
            normalize_for_accented_lookup("Nấm bào ngư"),
            "nấm bào ngư",
        )
        self.assertEqual(
            normalize_for_accentless_lookup("Nấm bào ngư"),
            "nam bao ngu",
        )

    def test_non_food_is_not_returned_as_an_ingredient(self):
        result = process_product(
            {"name": "Sữa tắm", "quantity": "1", "unit": "chai"},
            "2026-10-12",
        )

        self.assertIsNone(result)

    def test_database_exact_match_has_priority(self):
        result = process_product({"name": "Mắm ruốc Huế"})

        self.assertEqual(
            result["ingredient_id"],
            "643f4c19-4f2b-4e3e-90a4-de6c23d95b1e",
        )
        self.assertEqual(result["name"], "Mắm ruốc huế")

    def test_dual_bm25_returns_only_the_public_json_shape(self):
        result = process_product(
            {
                "name": "Bơ lạt Anchor",
                "quantity": "2",
                "unit": "hộp",
            },
            "2026-10-12",
        )

        self.assertEqual(
            result,
            {
                "name": "Bơ lạt",
                "ingredient_id": "321bb46a-7bfe-40f2-8040-dcc400e3886f",
                "qty_display": "2",
                "unit_display": "hộp",
                "purchased_at": "2026-10-12",
                "expiry_date": "2026-10-12",
            },
        )

    def test_unknown_product_is_not_returned(self):
        result = process_product({"name": "zzzz qqqq"})

        self.assertIsNone(result)

    def test_top_one_is_used_without_a_confidence_gate(self):
        result = process_product(
            {
                "name": "Hạt nêm Aji Ngon heo",
                "quantity": "1",
                "unit": "gói",
            },
            "2026-10-12",
        )

        self.assertEqual(
            result["ingredient_id"],
            "e8b805b4-9499-4c48-b582-f4e045be55b6",
        )

    def test_batch_uses_one_date_and_receipt_units(self):
        result = process_products(
            [
                {"name": "Mắm ruốc Huế", "quantity": "2", "unit": "hũ"},
                {"name": "Sả băm", "quantity": "3", "unit": "gói"},
                {"name": "Sữa tắm", "quantity": "1", "unit": "chai"},
                {"name": "zzzz qqqq", "quantity": "1", "unit": None},
            ]
        )

        self.assertEqual(len(result["items"]), 2)
        self.assertEqual(result["items"][0]["unit_display"], "hũ")
        self.assertEqual(result["items"][1]["unit_display"], "gói")
        self.assertEqual(
            result["items"][0]["purchased_at"],
            result["items"][1]["purchased_at"],
        )
        self.assertEqual(result["ignored_non_food_count"], 1)
        self.assertEqual(result["unmatched_item_count"], 1)


if __name__ == "__main__":
    unittest.main()
