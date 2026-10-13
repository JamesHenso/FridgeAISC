import math
from collections import Counter
from dataclasses import dataclass, replace
from typing import Callable, Iterable


@dataclass(frozen=True)
class SearchDocument:
    """A canonical product that can be returned by a retriever."""

    id: str
    name: str
    product_type: str
    metadata: dict


@dataclass(frozen=True)
class SearchResult:
    document: SearchDocument
    score: float
    query_coverage: float
    document_coverage: float
    matched_terms: tuple[str, ...]
    base_score: float | None = None
    score_details: dict | None = None


class BM25Retriever:
    """Small dependency-free BM25 implementation for the product catalogue."""

    def __init__(
        self,
        documents: Iterable[SearchDocument],
        tokenizer: Callable[[str], list[str]],
        *,
        k1: float = 1.5,
        b: float = 0.75,
    ):
        self.documents = list(documents)
        self.tokenizer = tokenizer
        self.k1 = k1
        self.b = b

        self._tokens = [
            tokenizer(document.name)
            for document in self.documents
        ]
        self._term_frequencies = [
            Counter(tokens)
            for tokens in self._tokens
        ]
        self._document_frequencies = Counter()

        for tokens in self._tokens:
            self._document_frequencies.update(set(tokens))

        self._average_document_length = (
            sum(map(len, self._tokens)) / len(self._tokens)
            if self._tokens
            else 0.0
        )

    def _idf(self, term: str) -> float:
        document_count = len(self.documents)
        frequency = self._document_frequencies.get(term, 0)

        # Robertson/Sparck Jones IDF with a +1 variant, keeping IDF positive.
        return math.log(
            1.0
            + (
                document_count - frequency + 0.5
            ) / (frequency + 0.5)
        )

    def search(
        self,
        query: str,
        *,
        top_k: int = 5,
    ) -> list[SearchResult]:
        if top_k <= 0 or not self.documents:
            return []

        query_tokens = self.tokenizer(query)
        if not query_tokens:
            return []

        unique_query_terms = set(query_tokens)
        results = []

        for index, document in enumerate(self.documents):
            document_tokens = self._tokens[index]
            term_frequencies = self._term_frequencies[index]
            document_length = len(document_tokens)
            score = 0.0

            for term in unique_query_terms:
                term_frequency = term_frequencies.get(term, 0)
                if not term_frequency:
                    continue

                length_normalization = 1.0 - self.b
                if self._average_document_length:
                    length_normalization += (
                        self.b
                        * document_length
                        / self._average_document_length
                    )

                denominator = (
                    term_frequency
                    + self.k1 * length_normalization
                )
                score += self._idf(term) * (
                    term_frequency * (self.k1 + 1.0)
                ) / denominator

            if score <= 0:
                continue

            unique_document_terms = set(document_tokens)
            matched_terms = tuple(
                sorted(unique_query_terms & unique_document_terms)
            )

            results.append(
                SearchResult(
                    document=document,
                    score=score,
                    query_coverage=(
                        len(matched_terms) / len(unique_query_terms)
                    ),
                    document_coverage=(
                        len(matched_terms) / len(unique_document_terms)
                        if unique_document_terms
                        else 0.0
                    ),
                    matched_terms=matched_terms,
                )
            )

        results.sort(
            key=lambda result: (
                result.score,
                result.document_coverage,
                result.query_coverage,
                -len(self.tokenizer(result.document.name)),
                result.document.id,
            ),
            reverse=True,
        )

        return results[:top_k]


class BM25CoverageRetriever(BM25Retriever):
    """BM25 followed by a catalogue-name coverage reranker.

    Receipt descriptions commonly contain brands and packaging words. Boosting
    candidates whose canonical tokens are covered by the query helps a generic
    catalogue entry such as "nước mắm" beat an unrelated longer entry that
    happens to share a rare token.
    """

    def __init__(
        self,
        documents: Iterable[SearchDocument],
        tokenizer: Callable[[str], list[str]],
        *,
        coverage_power: float = 1.5,
        k1: float = 1.5,
        b: float = 0.75,
    ):
        super().__init__(documents, tokenizer, k1=k1, b=b)
        self.coverage_power = coverage_power

    def search(
        self,
        query: str,
        *,
        top_k: int = 5,
    ) -> list[SearchResult]:
        bm25_results = super().search(
            query,
            top_k=len(self.documents),
        )
        reranked = [
            replace(
                result,
                score=(
                    result.score
                    * result.document_coverage ** self.coverage_power
                ),
                base_score=result.score,
            )
            for result in bm25_results
        ]
        reranked.sort(
            key=lambda result: (
                result.score,
                result.document_coverage,
                result.query_coverage,
                result.document.id,
            ),
            reverse=True,
        )
        return reranked[:top_k]


class DualFieldBM25Retriever:
    """Combine accent-aware BM25 with an accentless OCR fallback.

    When a query contains Vietnamese diacritics, the accent-aware index is the
    primary signal and the accentless index receives a smaller weight. For an
    unaccented query, only the accentless index is used. The merged score is
    then reranked by weighted canonical-name coverage.
    """

    def __init__(
        self,
        documents: Iterable[SearchDocument],
        accented_tokenizer: Callable[[str], list[str]],
        accentless_tokenizer: Callable[[str], list[str]],
        has_diacritics: Callable[[str], bool],
        *,
        accented_weight: float = 1.0,
        accentless_fallback_weight: float = 0.3,
        coverage_power: float = 1.5,
        k1: float = 1.5,
        b: float = 0.75,
    ):
        self.documents = list(documents)
        self.has_diacritics = has_diacritics
        self.accented_weight = accented_weight
        self.accentless_fallback_weight = accentless_fallback_weight
        self.coverage_power = coverage_power
        self.accented = BM25Retriever(
            self.documents,
            accented_tokenizer,
            k1=k1,
            b=b,
        )
        self.accentless = BM25Retriever(
            self.documents,
            accentless_tokenizer,
            k1=k1,
            b=b,
        )

    def search(
        self,
        query: str,
        *,
        top_k: int = 5,
    ) -> list[SearchResult]:
        if top_k <= 0:
            return []

        query_has_diacritics = self.has_diacritics(query)
        accented_weight = (
            self.accented_weight
            if query_has_diacritics
            else 0.0
        )
        accentless_weight = (
            self.accentless_fallback_weight
            if query_has_diacritics
            else 1.0
        )
        total_weight = accented_weight + accentless_weight

        accented_results = {
            result.document.id: result
            for result in self.accented.search(
                query,
                top_k=len(self.documents),
            )
        }
        accentless_results = {
            result.document.id: result
            for result in self.accentless.search(
                query,
                top_k=len(self.documents),
            )
        }

        document_ids = set(accented_results) | set(accentless_results)
        merged = []

        for document_id in document_ids:
            accented = accented_results.get(document_id)
            accentless = accentless_results.get(document_id)
            document = (
                accented.document
                if accented is not None
                else accentless.document
            )
            accented_score = accented.score if accented else 0.0
            accentless_score = accentless.score if accentless else 0.0
            combined_bm25_score = (
                accented_weight * accented_score
                + accentless_weight * accentless_score
            )

            query_coverage = (
                accented_weight
                * (accented.query_coverage if accented else 0.0)
                + accentless_weight
                * (accentless.query_coverage if accentless else 0.0)
            ) / total_weight
            document_coverage = (
                accented_weight
                * (accented.document_coverage if accented else 0.0)
                + accentless_weight
                * (accentless.document_coverage if accentless else 0.0)
            ) / total_weight
            score = (
                combined_bm25_score
                * document_coverage ** self.coverage_power
            )

            if score <= 0:
                continue

            evidence = accented if accented_weight and accented else accentless
            merged.append(
                SearchResult(
                    document=document,
                    score=score,
                    base_score=combined_bm25_score,
                    query_coverage=query_coverage,
                    document_coverage=document_coverage,
                    matched_terms=(
                        evidence.matched_terms
                        if evidence is not None
                        else ()
                    ),
                    score_details={
                        "query_has_diacritics": query_has_diacritics,
                        "accented_bm25": accented_score,
                        "accentless_bm25": accentless_score,
                        "accented_weight": accented_weight,
                        "accentless_weight": accentless_weight,
                    },
                )
            )

        merged.sort(
            key=lambda result: (
                result.score,
                result.document_coverage,
                result.query_coverage,
                result.document.id,
            ),
            reverse=True,
        )
        return merged[:top_k]


class CharNgramTfidfRetriever:
    """Character n-gram TF-IDF with cosine similarity.

    Character fragments remain partially shared when OCR substitutes, drops,
    or joins characters, making this retriever complementary to token BM25.
    """

    def __init__(
        self,
        documents: Iterable[SearchDocument],
        normalizer: Callable[[str], str],
        *,
        ngram_range: tuple[int, int] = (2, 5),
    ):
        from sklearn.feature_extraction.text import TfidfVectorizer

        self.documents = list(documents)
        self.normalizer = normalizer
        self.vectorizer = TfidfVectorizer(
            analyzer="char_wb",
            ngram_range=ngram_range,
            lowercase=False,
            sublinear_tf=True,
            norm="l2",
        )
        self._normalized_documents = [
            normalizer(document.name)
            for document in self.documents
        ]
        self._document_matrix = self.vectorizer.fit_transform(
            self._normalized_documents
        )

    def search(
        self,
        query: str,
        *,
        top_k: int = 5,
    ) -> list[SearchResult]:
        if top_k <= 0:
            return []

        normalized_query = self.normalizer(query)
        if not normalized_query:
            return []

        query_vector = self.vectorizer.transform([normalized_query])
        scores = (
            self._document_matrix @ query_vector.T
        ).toarray().ravel()
        query_terms = set(normalized_query.split())
        results = []

        for index, raw_score in enumerate(scores):
            score = float(raw_score)
            if score <= 0:
                continue

            document_terms = set(
                self._normalized_documents[index].split()
            )
            matched_terms = tuple(sorted(query_terms & document_terms))
            results.append(
                SearchResult(
                    document=self.documents[index],
                    score=score,
                    query_coverage=(
                        len(matched_terms) / len(query_terms)
                        if query_terms
                        else 0.0
                    ),
                    document_coverage=(
                        len(matched_terms) / len(document_terms)
                        if document_terms
                        else 0.0
                    ),
                    matched_terms=matched_terms,
                    score_details={
                        "char_ngram_tfidf": score,
                    },
                )
            )

        results.sort(
            key=lambda result: (
                result.score,
                result.document_coverage,
                result.query_coverage,
                result.document.id,
            ),
            reverse=True,
        )
        return results[:top_k]


class FuzzyWRatioRetriever:
    """RapidFuzz WRatio over normalized product names."""

    def __init__(
        self,
        documents: Iterable[SearchDocument],
        normalizer: Callable[[str], str],
    ):
        from rapidfuzz import fuzz

        self.documents = list(documents)
        self.normalizer = normalizer
        self._wratio = fuzz.WRatio
        self._normalized_documents = [
            normalizer(document.name)
            for document in self.documents
        ]

    def search(
        self,
        query: str,
        *,
        top_k: int = 5,
    ) -> list[SearchResult]:
        if top_k <= 0:
            return []

        normalized_query = self.normalizer(query)
        if not normalized_query:
            return []

        query_terms = set(normalized_query.split())
        results = []

        for index, normalized_document in enumerate(
            self._normalized_documents
        ):
            # Normalize WRatio from [0, 100] to [0, 1].
            score = self._wratio(
                normalized_query,
                normalized_document,
                processor=None,
            ) / 100.0
            if score <= 0:
                continue

            document_terms = set(normalized_document.split())
            matched_terms = tuple(sorted(query_terms & document_terms))
            results.append(
                SearchResult(
                    document=self.documents[index],
                    score=score,
                    query_coverage=(
                        len(matched_terms) / len(query_terms)
                        if query_terms
                        else 0.0
                    ),
                    document_coverage=(
                        len(matched_terms) / len(document_terms)
                        if document_terms
                        else 0.0
                    ),
                    matched_terms=matched_terms,
                    score_details={"fuzzy_wratio": score},
                )
            )

        results.sort(
            key=lambda result: (
                result.score,
                result.document_coverage,
                result.query_coverage,
                result.document.id,
            ),
            reverse=True,
        )
        return results[:top_k]


class ReciprocalRankFusionRetriever:
    """Fuse heterogeneous retrievers without mixing their raw score scales."""

    def __init__(
        self,
        retrievers: dict[str, object],
        *,
        rrf_k: int = 60,
        candidate_depth: int = 50,
        weights: dict[str, float] | None = None,
    ):
        if not retrievers:
            raise ValueError("At least one retriever is required")

        self.retrievers = retrievers
        self.rrf_k = rrf_k
        self.candidate_depth = candidate_depth
        self.weights = {
            name: (
                weights.get(name, 1.0)
                if weights is not None
                else 1.0
            )
            for name in retrievers
        }

    def search(
        self,
        query: str,
        *,
        top_k: int = 5,
    ) -> list[SearchResult]:
        if top_k <= 0:
            return []

        component_results = {
            name: retriever.search(
                query,
                top_k=self.candidate_depth,
            )
            for name, retriever in self.retrievers.items()
        }
        evidence_by_document = {}

        for name, results in component_results.items():
            for rank, result in enumerate(results, start=1):
                evidence = evidence_by_document.setdefault(
                    result.document.id,
                    {
                        "document": result.document,
                        "rrf_score": 0.0,
                        "ranks": {},
                        "results": [],
                    },
                )
                evidence["rrf_score"] += (
                    self.weights[name] / (self.rrf_k + rank)
                )
                evidence["ranks"][name] = rank
                evidence["results"].append(result)

        fused = []
        for evidence in evidence_by_document.values():
            source_results = evidence["results"]
            matched_terms = tuple(
                sorted(
                    {
                        term
                        for result in source_results
                        for term in result.matched_terms
                    }
                )
            )
            fused.append(
                SearchResult(
                    document=evidence["document"],
                    score=evidence["rrf_score"],
                    query_coverage=max(
                        result.query_coverage
                        for result in source_results
                    ),
                    document_coverage=max(
                        result.document_coverage
                        for result in source_results
                    ),
                    matched_terms=matched_terms,
                    score_details={
                        "rrf_k": self.rrf_k,
                        **{
                            f"rank_{name}": evidence["ranks"].get(name)
                            for name in self.retrievers
                        },
                        **{
                            f"weight_{name}": self.weights[name]
                            for name in self.retrievers
                        },
                    },
                )
            )

        fused.sort(
            key=lambda result: (
                result.score,
                result.document_coverage,
                result.query_coverage,
                result.document.id,
            ),
            reverse=True,
        )
        return fused[:top_k]
