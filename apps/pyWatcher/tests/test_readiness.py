import asyncio
import pytest
from utils.readiness import file_is_ready, check_ready
from models.event import FileReadyEvent

@pytest.mark.asyncio
async def test_check_ready_creates_event(tmp_path):
    file_path = tmp_path / "test.csv"
    file_path.write_text("dummy")

    events = []
    def fake_insert(db, ev):
        events.append(ev)

    ev = await check_ready(db=None,
                           ref_id="ref123",
                           file_name="test.csv",
                           file_uri=str(file_path),
                           insert_event=fake_insert)

    assert isinstance(ev, FileReadyEvent)
    assert ev.referenceId == "ref123"
    assert events[0] == ev
