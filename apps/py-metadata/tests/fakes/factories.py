# tests/fakes/factories.py

import uuid
import json
from models.event import EventMetadata, MetadataResult, MetadataSearchResultEvent, SearchResult, Summary
from utils.time import utc_now
from models.enums import MediaType, TaskStatus
from models.metadata import Metadata

def make_event() -> MetadataSearchResultEvent:
    return MetadataSearchResultEvent(
        referenceId=uuid.uuid4(),
        eventId=uuid.uuid4(),
        metadata=EventMetadata(
            created=utc_now(),
            derivedFromId={uuid.uuid4()}
        ),
        results=[],
        recommended=SearchResult(
            searchTitles=["Foo"],
            similarity=85,
            prefix=10,
            keywordScore=40.0,
            typeScore=80.0,
            completenessScore=15.0,
            sourceScore=5.0,
            totalScore=230.0,
            metadata=MetadataResult(
                source="test",
                title="title",
                alternateTitles=[],
                cover=None,
                bannerImage=None,
                type=MediaType.SERIE,
                summary=[Summary(language="en", description="desc")],
                genres=["action"]
            )
        ),
        status=TaskStatus.PENDING
    )





# -------------------------------------------------------------------
# TASKS table rows
# -------------------------------------------------------------------

def make_task_row(
    task_id=None,
    reference_id=None,
    search_titles=None,
    collection="anime",
    media_type=MediaType.MOVIE,
    claimed=False,
    consumed=False,
):
    return {
        "REFERENCE_ID": str(reference_id or uuid.uuid4()),
        "TASK_ID": str(task_id or uuid.uuid4()),
        "TASK": "MetadataSearchTask",
        "STATUS": TaskStatus.PENDING.value,
        "DATA": json.dumps({
            "data": {
                "searchTitles": search_titles or ["Foo", "Bar"],
                "collection": collection,
                "mediaType": media_type.value,
            }
        }),
        "CLAIMED": claimed,
        "CLAIMED_BY": None,
        "CONSUMED": consumed,
        "LAST_CHECK_IN": None,
        "PERSISTED_AT": utc_now().strftime("%Y-%m-%d %H:%M:%S.%f"),
    }


# -------------------------------------------------------------------
# METADATA table rows
# -------------------------------------------------------------------

def make_metadata_row(
    source="aniiv2",
    source_id="12345",
    title="My Anime",
    media_type=MediaType.SERIE,
):
    return {
        "ID": 1,
        "SOURCE": source,
        "SOURCE_ID": source_id,
        "TITLE": title,
        "COVER": "http://example.com/cover.jpg",
        "BANNER_IMAGE": "http://example.com/banner.jpg",
        "MEDIA_TYPE": media_type.value,
        "LAST_UPDATED": "2024-01-01 12:00:00",
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
# Full Metadata object (Python model)
# -------------------------------------------------------------------

def make_metadata_model(source="mal", title="Dummy Title"):
    return Metadata(
        sourceId=1,
        title=title,
        altTitle=[f"{title} alt"],
        cover="http://example.com/cover.jpg",
        bannerImage=None,
        type=MediaType.MOVIE,
        summary=[Summary(summary="A fake summary", language="en")],
        genres=["Drama", "Action"],
        source=source,
    )
