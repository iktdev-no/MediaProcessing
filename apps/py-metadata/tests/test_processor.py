import uuid
import pytest

import worker.processor as processor
from models.task import MetadataSearchTask, MetadataSearchData, TaskStatus
from models.metadata import Metadata, Summary, MediaType
from models.event import MetadataResult
from utils.time import utc_now
from tests.fakes.fake_db import FakeDB


# --- Helpers ----------------------------------------------------

def make_dummy_metadata(source="mal", title="Foo"):
    return Metadata(
        sourceId=1,
        title=title,
        altTitle=[],
        cover="cover.jpg",
        bannerImage=None,
        type=MediaType.MOVIE,
        summary=[Summary(summary="A fake summary", language="en")],
        genres=["Drama"],
        source=source,
    )


def make_dummy_task():
    return MetadataSearchTask(
        referenceId=uuid.uuid4(),
        taskId=uuid.uuid4(),
        task="MetadataSearchTask",
        status=TaskStatus.PENDING,
        data=MetadataSearchData(
            searchTitles=["Foo"],
            collection="bar",
            mediaType=MediaType.MOVIE
        ),
        claimed=False,
        claimedBy=None,
        consumed=False,
        lastCheckIn=None,
        persistedAt=utc_now()
    )


# --- Tests ------------------------------------------------------

@pytest.mark.asyncio
async def test_process_task_success(monkeypatch):
    async def good_search(db, titles):
        return [
            make_dummy_metadata("mal", "Foo"),
            make_dummy_metadata("imdb", "Foo Movie"),
        ]

    monkeypatch.setattr(processor, "run_search", good_search)

    called = {}
    monkeypatch.setattr(
        processor,
        "mark_failed",
        lambda db, tid: called.setdefault("failed", True),
    )

    task = make_dummy_task()
    event = await processor.process_task(FakeDB(), task)

    assert isinstance(event, processor.MetadataSearchResultEvent)
    assert event.status == TaskStatus.COMPLETED
    assert event.recommended is not None
    assert "failed" not in called


@pytest.mark.asyncio
async def test_process_task_no_results(monkeypatch):
    async def empty_search(db, titles):
        return []

    monkeypatch.setattr(processor, "run_search", empty_search)

    called = {}
    monkeypatch.setattr(
        processor,
        "mark_failed",
        lambda db, tid: called.setdefault("failed", True),
    )

    task = make_dummy_task()
    event = await processor.process_task(FakeDB(), task)

    assert event is None
    assert "failed" in called


@pytest.mark.asyncio
async def test_process_task_exception(monkeypatch):
    async def bad_search(db, titles):
        raise RuntimeError("boom")

    monkeypatch.setattr(processor, "run_search", bad_search)

    called = {}
    monkeypatch.setattr(
        processor,
        "mark_failed",
        lambda db, tid: called.setdefault("failed", True),
    )

    task = make_dummy_task()
    event = await processor.process_task(FakeDB(), task)

    assert event is None
    assert "failed" in called


@pytest.mark.asyncio
async def test_choose_recommended_prefers_highest_total():
    r1 = processor.SearchResult(
        searchTitles=["Foo"],
        similarity=10,
        prefix=5,
        keywordScore=0.0,
        typeScore=0.0,
        completenessScore=0.0,
        sourceScore=0.0,
        totalScore=300,
        metadata=MetadataResult(
            source="mal",
            title="Foo",
            alternateTitles=[],
            cover="",
            bannerImage=None,
            type=MediaType.MOVIE,
            summary=[],
            genres=[],
        ),
    )

    r2 = processor.SearchResult(
        searchTitles=["Foo"],
        similarity=20,
        prefix=5,
        keywordScore=0.0,
        typeScore=0.0,
        completenessScore=0.0,
        sourceScore=0.0,
        totalScore=200,
        metadata=MetadataResult(
            source="imdb",
            title="Foo",
            alternateTitles=[],
            cover="",
            bannerImage=None,
            type=MediaType.MOVIE,
            summary=[],
            genres=[],
        ),
    )

    r3 = processor.SearchResult(
        searchTitles=["Foo"],
        similarity=30,
        prefix=5,
        keywordScore=0.0,
        typeScore=0.0,
        completenessScore=0.0,
        sourceScore=0.0,
        totalScore=250,
        metadata=MetadataResult(
            source="anii",
            title="Foo",
            alternateTitles=[],
            cover="",
            bannerImage=None,
            type=MediaType.MOVIE,
            summary=[],
            genres=[],
        ),
    )

    recommended = processor.choose_recommended([r1, r2, r3])
    assert recommended is r1

