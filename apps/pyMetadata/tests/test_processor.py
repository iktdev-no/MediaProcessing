import asyncio
import uuid
from datetime import datetime
import pytest

import worker.processor as processor
from models.task import MetadataSearchTask, MetadataSearchData, TaskStatus
from models.metadata import Metadata, Summary, MediaType

# --- Helpers ---
def make_dummy_metadata(source="mal", title="Foo"):
    return Metadata(
        title=title,
        altTitle=[],
        cover="cover.jpg",
        banner=None,
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
        data=MetadataSearchData(searchTitles=["Foo"], collection="bar"),
        claimed=False,
        claimedBy=None,
        consumed=False,
        lastCheckIn=None,
        persistedAt=datetime.now()
    )

# --- Tests ---

@pytest.mark.asyncio
async def test_process_task_success(monkeypatch):
    # Async stub for run_search
    async def good_search(titles):
        return [make_dummy_metadata("mal"), make_dummy_metadata("imdb")]

    monkeypatch.setattr(processor, "run_search", good_search)

    # Matchers return fixed scores
    class DummyMatcher:
        def __init__(self, title, m): pass
        def getScore(self): return 50
    monkeypatch.setattr(processor, "SimpleMatcher", DummyMatcher)
    monkeypatch.setattr(processor, "PrefixMatcher", DummyMatcher)
    monkeypatch.setattr(processor, "AdvancedMatcher", DummyMatcher)

    # Fake DB and mark_failed
    class FakeDB: pass
    called = {}
    monkeypatch.setattr(processor, "mark_failed", lambda db, tid: called.setdefault("failed", True))

    task = make_dummy_task()
    event = await processor.process_task(FakeDB(), task)

    assert isinstance(event, processor.MetadataSearchResultEvent)
    assert event.status == TaskStatus.COMPLETED
    assert event.recommended is not None
    assert "failed" not in called


@pytest.mark.asyncio
async def test_process_task_no_results(monkeypatch):
    async def empty_search(titles):
        return []
    monkeypatch.setattr(processor, "run_search", empty_search)

    class FakeDB: pass
    called = {}
    monkeypatch.setattr(processor, "mark_failed", lambda db, tid: called.setdefault("failed", True))

    task = make_dummy_task()
    event = await processor.process_task(FakeDB(), task)

    assert event is None
    assert "failed" in called


@pytest.mark.asyncio
async def test_process_task_exception(monkeypatch):
    async def bad_search(titles):
        raise RuntimeError("boom")
    monkeypatch.setattr(processor, "run_search", bad_search)

    class FakeDB: pass
    called = {}
    monkeypatch.setattr(processor, "mark_failed", lambda db, tid: called.setdefault("failed", True))

    task = make_dummy_task()
    event = await processor.process_task(FakeDB(), task)

    assert event is None
    assert "failed" in called



@pytest.mark.asyncio
async def test_choose_recommended_prefers_advanced(monkeypatch):
    # Lag tre SearchResult med ulike scorer
    m = make_dummy_metadata("mal")
    r1 = processor.SearchResult(simpleScore=10, prefixScore=10, advancedScore=90, sourceWeight=1.0, metadata=processor.MetadataResult(source="mal", title="Foo", alternateTitles=[], cover="", bannerImage=None, type=MediaType.MOVIE, summary=[], genres=[]))
    r2 = processor.SearchResult(simpleScore=50, prefixScore=50, advancedScore=20, sourceWeight=1.0, metadata=processor.MetadataResult(source="imdb", title="Foo", alternateTitles=[], cover="", bannerImage=None, type=MediaType.MOVIE, summary=[], genres=[]))
    r3 = processor.SearchResult(simpleScore=80, prefixScore=80, advancedScore=80, sourceWeight=1.0, metadata=processor.MetadataResult(source="anii", title="Foo", alternateTitles=[], cover="", bannerImage=None, type=MediaType.MOVIE, summary=[], genres=[]))

    recommended = processor.choose_recommended([r1, r2, r3])
    assert recommended is r1  # høyest advancedScore vinner
