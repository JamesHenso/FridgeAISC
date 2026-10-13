import argparse
import csv
from pathlib import Path

from product_processor import (
    DUAL_BM25_RETRIEVER,
    SEARCH_DOCUMENTS,
    normalize_for_accentless_lookup,
    tokenize_without_accents,
)
from retrieval import (
    BM25CoverageRetriever,
    BM25Retriever,
    CharNgramTfidfRetriever,
    FuzzyWRatioRetriever,
    ReciprocalRankFusionRetriever,
)


BASE_DIR = Path(__file__).resolve().parent
DEFAULT_CASES_FILE = BASE_DIR / "retrieval_test_cases.csv"


BM25_RETRIEVER = BM25Retriever(
    SEARCH_DOCUMENTS,
    tokenizer=tokenize_without_accents,
)
BM25_COVERAGE_RETRIEVER = BM25CoverageRetriever(
    SEARCH_DOCUMENTS,
    tokenizer=tokenize_without_accents,
)
CHAR_NGRAM_RETRIEVER = CharNgramTfidfRetriever(
    SEARCH_DOCUMENTS,
    normalizer=normalize_for_accentless_lookup,
)
FUZZY_WRATIO_RETRIEVER = FuzzyWRatioRetriever(
    SEARCH_DOCUMENTS,
    normalizer=normalize_for_accentless_lookup,
)
HYBRID_RRF_RETRIEVER = ReciprocalRankFusionRetriever(
    {
        "bm25": DUAL_BM25_RETRIEVER,
        "char_ngram": CHAR_NGRAM_RETRIEVER,
        "fuzzy": FUZZY_WRATIO_RETRIEVER,
    },
    weights={
        "bm25": 2.0,
        "char_ngram": 2.0,
        "fuzzy": 0.5,
    },
)


def load_cases(path: Path) -> list[dict]:
    with open(path, encoding="utf-8-sig", newline="") as file:
        return list(csv.DictReader(file))


RETRIEVERS = {
    "bm25": BM25_RETRIEVER,
    "bm25-coverage": BM25_COVERAGE_RETRIEVER,
    "dual-bm25": DUAL_BM25_RETRIEVER,
    "char-ngram": CHAR_NGRAM_RETRIEVER,
    "fuzzy-wratio": FUZZY_WRATIO_RETRIEVER,
    "hybrid-rrf": HYBRID_RRF_RETRIEVER,
}


def evaluate(path: Path, top_k: int, algorithm: str) -> int:
    cases = load_cases(path)
    retriever = RETRIEVERS[algorithm]
    top_1_hits = 0
    top_k_hits = 0
    reciprocal_rank_sum = 0.0

    print(
        f"{'query':38} {'expected':10} {'top result':10} "
        f"{'rank':5} {'score':>7}"
    )
    print("-" * 78)

    for case in cases:
        results = retriever.search(
            case["query"],
            top_k=top_k,
        )
        expected_id = case["expected_id"]
        result_ids = [result.document.id for result in results]
        rank = (
            result_ids.index(expected_id) + 1
            if expected_id in result_ids
            else None
        )

        if rank == 1:
            top_1_hits += 1
        if rank is not None:
            top_k_hits += 1
            reciprocal_rank_sum += 1.0 / rank

        top_result = results[0] if results else None
        print(
            f"{case['query'][:37]:38} "
            f"{expected_id:10} "
            f"{(top_result.document.id if top_result else '-'):10} "
            f"{(str(rank) if rank else '-'):5} "
            f"{(top_result.score if top_result else 0):7.3f}"
        )

    total = len(cases)
    if not total:
        print("No evaluation cases found.")
        return 1

    print(f"\nMetrics ({algorithm})")
    print(f"Top-1 accuracy : {top_1_hits / total:.2%} ({top_1_hits}/{total})")
    print(f"Top-{top_k} recall   : {top_k_hits / total:.2%} ({top_k_hits}/{total})")
    print(f"MRR            : {reciprocal_rank_sum / total:.4f}")

    return 0


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Evaluate product retrieval against labelled receipt queries."
    )
    parser.add_argument(
        "--cases",
        type=Path,
        default=DEFAULT_CASES_FILE,
        help="CSV with query, expected_id and expected_type columns.",
    )
    parser.add_argument("--top-k", type=int, default=5)
    parser.add_argument(
        "--algorithm",
        choices=sorted(RETRIEVERS),
        default="bm25",
    )
    args = parser.parse_args()

    return evaluate(args.cases, args.top_k, args.algorithm)


if __name__ == "__main__":
    raise SystemExit(main())
