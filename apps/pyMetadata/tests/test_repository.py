import json
import uuid
from datetime import datetime
import pytest
from db import repository
from models.event import MetadataSearchResultEvent, EventMetadata, SearchResult, MetadataResult, Summary
from models.enums import MediaType, TaskStatus
from db.repository import persist_event_and_mark_consumed
from models.task import MetadataSearchData, MetadataSearchTask

class FakeCursor:
    def __init__(self):
        self.executed = []
        self.rowcount = 1
    def execute(self, sql, params=None):
        self.executed.append((sql, params))
    def close(self): pass

class FakeConn:
    def __init__(self):
        self.cursor_obj = FakeCursor()
        self.committed = False
        self.rolled_back = False
    def cursor(self, dictionary=False):
        return self.cursor_obj
    def commit(self): self.committed = True
    def rollback(self): self.rolled_back = True

class FakeDB:
    def __init__(self):
        self.conn = FakeConn()

    def validate(self): pass
    

def make_event() -> MetadataSearchResultEvent:
    return MetadataSearchResultEvent(
        referenceId=uuid.uuid4(),
        eventId=uuid.uuid4(),
        metadata=EventMetadata(
            created=datetime.now(),
            derivedFromId={uuid.uuid4()}
        ),
        results=[],
        recommended=SearchResult(
            simpleScore=1,
            prefixScore=2,
            advancedScore=3,
            sourceWeight=1.0,
            metadata=MetadataResult(
                source="test",
                title="title",
                alternateTitles=None,
                cover=None,
                bannerImage=None,
                type=MediaType.SERIE,
                summary=[Summary(language="en", description="desc")],
                genres=["action"]
            )
        ),
        status=TaskStatus.PENDING
    )

def test_persist_event_and_mark_consumed_success():
    db = FakeDB()
    event = make_event()
    persist_event_and_mark_consumed(db, event, str(event.eventId))
    # verifiser at commit ble kalt
    assert db.conn.committed
    # verifiser at to SQL statements ble kjørt
    assert len(db.conn.cursor_obj.executed) == 2


def make_row(task_id, ref_id):
    # Simulerer en DB-rad slik den faktisk ligger i Tasks-tabellen
    return {
        "REFERENCE_ID": str(ref_id),
        "TASK_ID": str(task_id),
        "TASK": "MetadataSearchTask",
        "STATUS": TaskStatus.PENDING.value,
        "DATA": json.dumps({
            "searchTitles": ["Foo", "Bar"],
            "collection": "anime"
        }),
        "CLAIMED": False,
        "CLAIMED_BY": None,
        "CONSUMED": False,
        "LAST_CHECK_IN": None,
        "PERSISTED_AT": datetime.now().isoformat()
    }

def test_fetch_next_task_maps_correctly(monkeypatch):
    task_id = uuid.uuid4()
    ref_id = uuid.uuid4()
    fake_row = make_row(task_id, ref_id)

    # Fake DB som returnerer radene
    class FakeDB:
        def execute(self, query, *args, **kwargs):
            return [fake_row]

    # Monkeypatch fetch_next_task til å bruke fake_row direkte
    def fake_fetch_next_task(db):
        row = fake_row
        data = json.loads(row["DATA"])
        return MetadataSearchTask(
            referenceId=uuid.UUID(row["REFERENCE_ID"]),
            taskId=uuid.UUID(row["TASK_ID"]),
            task=row["TASK"],
            status=TaskStatus(row["STATUS"]),
            data=MetadataSearchData(
                searchTitles=data["searchTitles"],
                collection=data["collection"]
            ),
            claimed=row["CLAIMED"],
            claimedBy=row["CLAIMED_BY"],
            consumed=row["CONSUMED"],
            lastCheckIn=row["LAST_CHECK_IN"],
            persistedAt=datetime.fromisoformat(row["PERSISTED_AT"])
        )

    monkeypatch.setattr(repository, "fetch_next_task", fake_fetch_next_task)

    db = FakeDB()
    task = repository.fetch_next_task(db)

    # Verifiser at mappingen er korrekt
    assert isinstance(task, MetadataSearchTask)
    assert task.taskId == task_id
    assert task.referenceId == ref_id
    assert task.status == TaskStatus.PENDING
    assert task.data.collection == "anime"
    assert task.data.searchTitles == ["Foo", "Bar"]
    assert task.claimed is False
    assert task.consumed is False