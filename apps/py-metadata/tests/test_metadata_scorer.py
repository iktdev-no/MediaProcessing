import pytest
from algo.MetadataScorer import MetadataScorer, _dice_similarity, PRIORITY_MAP
from models.metadata import Metadata, Summary
from models.enums import MediaType


# -------------------------------------------------------------------
# Helpers
# -------------------------------------------------------------------

def make_metadata(
    title="My Anime",
    alt=["Alt Title"],
    source="mal",
    cover="cover.jpg",
    summary_text="summary",
    genres=["Action"],
    type=MediaType.MOVIE,
    sourceId=1
):
    return Metadata(
        sourceId=sourceId,
        title=title,
        altTitle=alt,
        cover=cover,
        bannerImage=None,
        type=type,
        summary=[Summary(summary=summary_text, language="en")],
        genres=genres,
        source=source,
    )


# -------------------------------------------------------------------
# Tests
# -------------------------------------------------------------------

def test_dice_similarity_basic():
    a = ["chainsaw", "man"]
    b = ["chainsaw", "man"]
    assert _dice_similarity(a, b) == 1.0

    c = ["chainsaw", "man"]
    d = ["chainsaw", "movie", "reze"]
    # overlap = 1, total = 2 + 3 = 5 → dice = 2/5 = 0.4
    assert _dice_similarity(c, d) == pytest.approx(0.4)


def test_scorer_basic():
    scorer = MetadataScorer()
    meta = make_metadata(title="My Anime")

    result = scorer.score(["My Anime"], meta, expected_type=MediaType.MOVIE)

    assert result.similarity == 100
    assert result.prefix == 10
    assert result.typeScore == 80
    assert result.completenessScore == 15

    # Keyword score should be a float (not testing exact value)
    assert isinstance(result.keywordScore, float)

    # Total score should be reasonably high
    assert result.totalScore > 100


def test_scorer_type_mismatch():
    scorer = MetadataScorer()
    meta = make_metadata(type=MediaType.MOVIE)

    result = scorer.score(["My Anime"], meta, expected_type=MediaType.SERIE)

    assert result.typeScore == -120

    # Mismatch should give lower score than match
    match_score = scorer.score(["My Anime"], meta, expected_type=MediaType.MOVIE)
    assert result.totalScore < match_score.totalScore


def test_scorer_source_priority():
    scorer = MetadataScorer()

    meta_mal = make_metadata(source="mal")
    meta_imdb = make_metadata(source="imdb")

    r1 = scorer.score(["My Anime"], meta_mal, expected_type=None)
    r2 = scorer.score(["My Anime"], meta_imdb, expected_type=None)

    # MAL and IMDb have different priority weights
    assert r1.sourceScore != r2.sourceScore


def test_scorer_completeness():
    scorer = MetadataScorer()

    meta_low = make_metadata(cover=None, summary_text="", genres=[])
    meta_high = make_metadata(cover="x", summary_text="y", genres=["z"])

    r_low = scorer.score(["My Anime"], meta_low, expected_type=None)
    r_high = scorer.score(["My Anime"], meta_high, expected_type=None)

    assert r_high.completenessScore > r_low.completenessScore
    assert r_high.totalScore > r_low.totalScore
