import asyncio
import pytest
import uuid
from datetime import datetime

from worker.search_runner import run_search
from models.metadata import Metadata, Summary, MediaType

# Dummy Metadata factory
def make_dummy_metadata(source: str, title: str = "Dummy Title") -> Metadata:
    return Metadata(
        title=title,
        altTitle=[f"{title} alt"],
        cover="http://example.com/cover.jpg",
        banner=None,
        type=MediaType.MOVIE,  # bruk en gyldig enum fra din kode
        summary=[Summary(summary="A fake summary", language="en")],
        genres=["Drama", "Action"],
        source=source,
    )

# Dummy Source that mimics SourceBase
class DummySource:
    def __init__(self, titles, result=None, raise_exc=False):
        self.titles = titles
        self._result = result
        self._raise_exc = raise_exc

    async def search(self):
        if self._raise_exc:
            raise RuntimeError("Search failed")
        return self._result

@pytest.mark.asyncio
async def test_run_search_all_results(monkeypatch):
    sources = [
        DummySource(["foo"], make_dummy_metadata("mal")),
        DummySource(["foo"], make_dummy_metadata("imdb")),
        DummySource(["foo"], make_dummy_metadata("anii")),
    ]
    monkeypatch.setattr("worker.search_runner.get_all_sources", lambda titles: sources)

    results = await run_search(["foo"])
    assert len(results) == 3
    assert all(isinstance(r, Metadata) for r in results)
    assert {r.source for r in results} == {"mal", "imdb", "anii"}

@pytest.mark.asyncio
async def test_run_search_filters_none(monkeypatch):
    sources = [
        DummySource(["foo"], make_dummy_metadata("mal")),
        DummySource(["foo"], None),
        DummySource(["foo"], make_dummy_metadata("imdb")),
    ]
    monkeypatch.setattr("worker.search_runner.get_all_sources", lambda titles: sources)

    results = await run_search(["foo"])
    assert len(results) == 2
    assert {r.source for r in results} == {"mal", "imdb"}

@pytest.mark.asyncio
async def test_run_search_handles_exception(monkeypatch):
    sources = [
        DummySource(["foo"], make_dummy_metadata("mal")),
        DummySource(["foo"], raise_exc=True),
        DummySource(["foo"], make_dummy_metadata("imdb")),
    ]
    monkeypatch.setattr("worker.search_runner.get_all_sources", lambda titles: sources)

    results = await run_search(["foo"])

    # Nå skal vi få bare de gyldige Metadata-resultatene
    assert all(isinstance(r, Metadata) for r in results)
    assert {r.source for r in results} == {"mal", "imdb"}

