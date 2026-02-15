import pytest

from tests.fakes.fake_db import FakeDB
from worker.search_runner import run_search
from models.metadata import Metadata, Summary, MediaType


# --- Helpers ----------------------------------------------------

def make_dummy_metadata(source: str, title: str = "Dummy Title") -> Metadata:
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


# Dummy Source that mimics SourceBase
class DummySource:
    def __init__(self, titles, result=None, raise_exc=False, name=None):
        self.titles = titles
        self._result = result
        self._raise_exc = raise_exc

        # SourceBase fields
        self.name = name or (result.source if result else "dummy")
        self.source = self.name
        self.found_ids = {}

    async def queryIds(self, title):
        if self._raise_exc:
            raise RuntimeError("queryIds failed")

        if not self._result:
            return {}

        # Return a fake ID for this source
        return {f"id_{self.name}": title}

    async def fetchMetadata(self, id):
        if self._raise_exc:
            raise RuntimeError("fetchMetadata failed")
        return self._result

    async def search(self):
        # Legacy compatibility
        return [self._result] if self._result else []


# --- Tests ------------------------------------------------------

@pytest.mark.asyncio
async def test_run_search_all_results(monkeypatch):
    sources = [
        DummySource(["foo"], make_dummy_metadata("mal")),
        DummySource(["foo"], make_dummy_metadata("imdb")),
        DummySource(["foo"], make_dummy_metadata("anii")),
    ]

    monkeypatch.setattr(
        "worker.search_runner.get_all_sources",
        lambda titles: sources
    )

    results = await run_search(FakeDB(), ["foo"])

    assert len(results) == 3
    assert all(isinstance(r, Metadata) for r in results)
    assert {r.source for r in results} == {"mal", "imdb", "anii"}


@pytest.mark.asyncio
async def test_run_search_filters_none(monkeypatch):
    sources = [
        DummySource(["foo"], make_dummy_metadata("mal")),
        DummySource(["foo"], None),  # no metadata
        DummySource(["foo"], make_dummy_metadata("imdb")),
    ]

    monkeypatch.setattr(
        "worker.search_runner.get_all_sources",
        lambda titles: sources
    )

    results = await run_search(FakeDB(), ["foo"])

    assert len(results) == 2
    assert {r.source for r in results} == {"mal", "imdb"}


@pytest.mark.asyncio
async def test_run_search_handles_exception(monkeypatch):
    sources = [
        DummySource(["foo"], make_dummy_metadata("mal")),
        DummySource(["foo"], raise_exc=True),
        DummySource(["foo"], make_dummy_metadata("imdb")),
    ]

    monkeypatch.setattr(
        "worker.search_runner.get_all_sources",
        lambda titles: sources
    )

    results = await run_search(FakeDB(), ["foo"])

    assert all(isinstance(r, Metadata) for r in results)
    assert {r.source for r in results} == {"mal", "imdb"}
