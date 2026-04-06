# algo/MetadataScorer.py

from typing import List, Optional
from dataclasses import dataclass

from models.metadata import Metadata
from models.event import SearchResult, MetadataResult, Summary
from models.enums import MediaType
from config.source_priority_loader import load_source_priority

PRIORITY_MAP = load_source_priority()


# ---------------------------------------------------------
# Token helpers
# ---------------------------------------------------------

def _clean(s: str) -> str:
    return s.lower().strip()


def _tokenize(s: str) -> List[str]:
    return (
        s.replace("–", " ")
         .replace("-", " ")
         .replace(":", " ")
         .lower()
         .split()
    )


def _all_meta_titles(m: Metadata) -> List[str]:
    titles = [m.title]
    if m.altTitle:
        titles.extend(m.altTitle)
    return titles


# ---------------------------------------------------------
# Sørensen–Dice similarity
# ---------------------------------------------------------

def _dice_similarity(a: List[str], b: List[str]) -> float:
    if not a or not b:
        return 0.0
    a_set = set(a)
    b_set = set(b)
    overlap = len(a_set & b_set)
    return (2 * overlap) / (len(a_set) + len(b_set))


# ---------------------------------------------------------
# Type, completeness, source
# ---------------------------------------------------------

def _media_type_score(expected: Optional[MediaType], actual: MediaType) -> float:
    if expected is None:
        return 0.0
    if expected == actual:
        return 80.0
    return -120.0


def _completeness(m: Metadata) -> float:
    score = 0.0
    if m.cover:
        score += 0.5
    if m.summary:
        score += 0.5
    if m.genres:
        score += 0.5
    return score * 10.0


def _source_priority(source: str) -> float:
    priority = PRIORITY_MAP.get(source.lower(), 10)
    return 10 - priority


# ---------------------------------------------------------
# Keyword scoring
# ---------------------------------------------------------

GENERIC_WORDS = {
    "the", "a", "an", "of", "and", "part",
    "season", "episode", "ep", "movie", "arc",
}

def _extract_keywords(title: str) -> List[str]:
    return [
        w for w in _tokenize(title)
        if w not in GENERIC_WORDS and len(w) > 2
    ]


def _keyword_score(search_keywords: List[str], meta_titles: List[str]) -> float:
    search_set = set(search_keywords)
    meta_words = set()

    for t in meta_titles:
        meta_words.update(_tokenize(t))

    score = 0.0

    # Bonus for spesifikke søkeord som finnes i metadata
    for kw in search_set:
        if kw in meta_words:
            score += 40.0

    # Straff for metadata-ord som ikke finnes i søket
    for mw in meta_words:
        if mw not in search_set and mw not in GENERIC_WORDS:
            score -= 1.0

    return score


# ---------------------------------------------------------
# SCORER
# ---------------------------------------------------------

class MetadataScorer:
    """
    Ren, deterministisk, token-basert scoring-motor.
    Bruker Sørensen–Dice, altTitles, streng keyword-penalty,
    sterk type-matching og svak prefix.
    """

    def score(self, titles: List[str], m: Metadata, expected_type: MediaType) -> SearchResult:
        # 1. Velg mest informative søketittel
        main_title = max(titles, key=len)
        search_tokens = _tokenize(main_title)

        # 2. Ekstraher keywords fra ALLE søketitler
        all_keywords = []
        for t in titles:
            all_keywords.extend(_extract_keywords(t))
        all_keywords = list(set(all_keywords))

        # 3. Metadata-titler
        meta_titles = _all_meta_titles(m)

        # --- Sørensen–Dice similarity ---
        dice = max(
            _dice_similarity(search_tokens, _tokenize(mt))
            for mt in meta_titles
        )

        # --- Weak prefix ---
        prefix = 0.0
        main_prefix = _clean(main_title).split(" ")[0]
        for mt in meta_titles:
            if _clean(mt).startswith(main_prefix):
                prefix = 10.0
                break

        # --- Type score ---
        type_score = _media_type_score(expected_type, m.type)

        # --- Completeness ---
        completeness_score = _completeness(m)

        # --- Source priority ---
        source_score = _source_priority(m.source)

        # --- Keyword score ---
        keyword_score = _keyword_score(all_keywords, meta_titles)

        # --- Total score ---
        total_score = (
            dice * 100.0 +     # Dice gir 0.0–1.0 → skaleres til 0–100
            prefix +
            type_score +
            completeness_score +
            source_score +
            keyword_score
        )

        metadata_result = MetadataResult(
            source=m.source,
            title=m.title,
            alternateTitles=m.altTitle or [],
            cover=m.cover,
            bannerImage=m.bannerImage,
            type=m.type,
            summary=[Summary(language=s.language, description=s.summary) for s in m.summary],
            genres=m.genres,
        )

        return SearchResult(
            searchTitles=titles,
            similarity=int(dice * 100),
            prefix=int(prefix),
            keywordScore=keyword_score,
            typeScore=type_score,
            completenessScore=completeness_score,
            sourceScore=source_score,
            totalScore=total_score,
            metadata=metadata_result
        )

