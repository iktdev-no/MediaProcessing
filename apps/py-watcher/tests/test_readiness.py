import asyncio
import pytest
from utils.readiness import check_ready
from models.event import FileReadyEvent

@pytest.mark.asyncio
async def test_check_ready_creates_event(tmp_path, monkeypatch):
    file_path = tmp_path / "test.csv"
    file_path.write_text("dummy")

    # async mock av file_is_ready
    async def fake_ready(path, wait=1.0):
        return True

    monkeypatch.setattr("utils.readiness.file_is_ready", fake_ready)

    events = []
    def fake_insert(db, ev):
        events.append(ev)

    ev = await check_ready(
        db=None,
        ref_id="ref123",
        file_name="test.csv",
        file_uri=str(file_path),
        insert_event=fake_insert,
        derived_from_event_id="evt-added-123"
    )

    assert isinstance(ev, FileReadyEvent)
    assert ev.referenceId == "ref123"
    assert ev.metadata.derivedFromId == {"evt-added-123"}
    assert events[0] == ev
