import pytest
from db.metadata_repository import fetch_metadata_by_source_and_id
from models.metadata import Metadata, Summary, MediaType
from tests.fakes.fake_db import FakeDB

# Bruk shared FakeDB


# -------------------------------------------------------------------
# Helpers for building fake DB rows
# -------------------------------------------------------------------

def make_metadata_row():
    return {
        "ID": 1,
        "SOURCE": "aniiv2",
        "SOURCE_ID": "12345",
        "TITLE": "My Anime",
        "COVER": "http://example.com/cover.jpg",
        "BANNER_IMAGE": "http://example.com/banner.jpg",
        "MEDIA_TYPE": "Serie",  # lowercase to match MediaType
        "LAST_UPDATED": "2024-01-01 12:00:00"
    }


def make_title_rows():
    return [
        {"TITLE": "My Anime Alt 1"},
        {"TITLE": "My Anime Alt 2"},
    ]


def make_summary_rows():
    return [
        {"LANGUAGE": "en", "DESCRIPTION": "English summary"},
        {"LANGUAGE": "jp", "DESCRIPTION": "Japanese summary"},
    ]


def make_genre_rows():
    return [
        {"GENRE": "Action"},
        {"GENRE": "Drama"},
    ]


# -------------------------------------------------------------------
# Tests
# -------------------------------------------------------------------

def test_fetch_metadata_success():
    rows_map = {
        "METADATA": [make_metadata_row()],
        "METADATA_TITLES": make_title_rows(),
        "METADATA_SUMMARIES": make_summary_rows(),
        "METADATA_GENRES": make_genre_rows(),
    }

    db = FakeDB(rows_map)

    result = fetch_metadata_by_source_and_id(db, "aniiv2", "12345")

    assert result is not None
    assert result.title == "My Anime"
    assert result.altTitle == ["My Anime Alt 1", "My Anime Alt 2"]
    assert len(result.summary) == 2
    assert result.summary[0].language == "en"
    assert result.summary[0].summary == "English summary"
    assert result.genres == ["Action", "Drama"]


def test_fetch_metadata_not_found():
    rows_map = {
        "METADATA": [],
        "METADATA_TITLES": [],
        "METADATA_SUMMARIES": [],
        "METADATA_GENRES": [],
    }

    db = FakeDB(rows_map)

    result = fetch_metadata_by_source_and_id(db, "aniiv2", "99999")
    assert result is None
