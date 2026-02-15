import json
import uuid
import pytest

from tests.fakes.factories import make_event, make_task_row
from utils.time import parse_mysql_ts, utc_now
from db import repository
from db.repository import persist_event_and_mark_consumed
from models.event import MetadataSearchResultEvent, EventMetadata, SearchResult, MetadataResult, Summary
from models.enums import MediaType, TaskStatus
from models.task import MetadataSearchData, MetadataSearchTask

# Use shared FakeDB
from tests.fakes.fake_db import FakeDB



def test_persist_event_and_mark_consumed_success():
    db = FakeDB()

    event = make_event()
    persist_event_and_mark_consumed(db, event, str(event.eventId))

    # verify commit
    assert db.conn.committed

    # verify two SQL statements executed
    assert len(db.conn._cursor.executed) == 2


def test_fetch_next_task_maps_correctly(monkeypatch):
    task_id = uuid.uuid4()
    ref_id = uuid.uuid4()
    fake_row = make_task_row(task_id, ref_id)

    # Fake DB with TASKS table populated
    rows_map = {
        "TASKS": [fake_row]
    }

    db = FakeDB(rows_map)

    # No monkeypatch needed — FakeDB handles SELECT routing
    task = repository.fetch_next_task(db)

    assert isinstance(task, MetadataSearchTask)
    assert task.taskId == task_id
    assert task.referenceId == ref_id
    assert task.status == TaskStatus.PENDING
    assert task.data.collection == "anime"
    assert task.data.searchTitles == ["Foo", "Bar"]
    assert task.claimed is False
    assert task.consumed is False

