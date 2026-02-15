import pytest
import uuid
import time

from models.event import MetadataSearchResultEvent, EventMetadata
from worker.poller import run_worker
from models.task import MetadataSearchTask, MetadataSearchData
from models.enums import MediaType, TaskStatus
from utils.time import utc_now

# Use shared FakeDB
from tests.fakes.fake_db import FakeDB


def make_dummy_event():
    return MetadataSearchResultEvent(
        referenceId=uuid.uuid4(),
        eventId=uuid.uuid4(),
        metadata=EventMetadata(
            created=utc_now(),
            derivedFromId={uuid.uuid4()}
        ),
        results=[],
        persistedAt=utc_now(),
        recommended=None,
        status="Completed"
    )


def make_task():
    return MetadataSearchTask(
        referenceId=uuid.uuid4(),
        taskId=uuid.uuid4(),
        task="MetadataSearchTask",
        status=TaskStatus.PENDING,
        data=MetadataSearchData(
            searchTitles=["foo"],
            collection="bar",
            mediaType=MediaType.MOVIE
        ),
        claimed=False,
        claimedBy=None,
        consumed=False,
        lastCheckIn=None,
        persistedAt=utc_now()
    )


def test_run_worker_processes_one(monkeypatch):
    events = []
    task = make_task()

    calls = {"n": 0}

    def fetch_once(db):
        if calls["n"] == 0:
            calls["n"] += 1
            return task
        calls["n"] += 1
        return None

    monkeypatch.setattr("worker.poller.fetch_next_task", fetch_once)
    monkeypatch.setattr("worker.poller.claim_task", lambda *a, **k: True)

    async def fake_process_task(db, task):
        return make_dummy_event()

    monkeypatch.setattr("worker.poller.process_task", fake_process_task)

    def persist_stub(db, event, task_id):
        events.append("dummy_event")

    monkeypatch.setattr("worker.poller.persist_event_and_mark_consumed", persist_stub)
    monkeypatch.setattr("worker.poller.mark_failed", lambda *a, **k: events.append("failed"))
    monkeypatch.setattr("worker.poller.time.sleep", lambda s: None)
    monkeypatch.setattr("worker.poller.time.time", lambda: 123)

    dummy_hb = lambda ts, in_backoff=False, error=None: None

    run_worker(
        db=FakeDB(),
        shutdown_flag_ref=lambda: calls["n"] >= 2,
        heartbeat_ref=dummy_hb
    )

    assert "dummy_event" in events


def test_backoff(monkeypatch):
    intervals = []

    monkeypatch.setattr("worker.poller.fetch_next_task", lambda db: None)
    monkeypatch.setattr(time, "sleep", lambda s: intervals.append(s))

    dummy_hb = lambda ts, in_backoff=False, error=None: None

    run_worker(
        db=FakeDB(),
        shutdown_flag_ref=lambda: len(intervals) >= 4,
        heartbeat_ref=dummy_hb
    )

    assert intervals == [5, 10, 20, 40]


def test_backoff_on_connection_error(monkeypatch):
    intervals = []
    reconnects = []

    class FakeDBReconnect(FakeDB):
        def connect(self):
            reconnects.append("reconnect")

    monkeypatch.setattr(
        "worker.poller.fetch_next_task",
        lambda db: (_ for _ in ()).throw(RuntimeError("lost"))
    )

    monkeypatch.setattr(time, "sleep", lambda s: intervals.append(s))

    dummy_hb = lambda ts, in_backoff=False, error=None: None

    run_worker(
        db=FakeDBReconnect(),
        shutdown_flag_ref=lambda: len(reconnects) >= 2,
        heartbeat_ref=dummy_hb
    )

    assert reconnects == ["reconnect", "reconnect"]
    assert intervals == [5, 5]


def test_backoff_enter_exit(monkeypatch):
    calls = []

    seq = [True, False, True]

    def fake_fetch(db):
        return make_task() if seq.pop(0) else None

    import worker.poller as poller

    monkeypatch.setattr("worker.poller.fetch_next_task", fake_fetch)
    monkeypatch.setattr("worker.poller.claim_task", lambda *a, **k: True)

    async def fake_process_task(db, task):
        return make_dummy_event()

    monkeypatch.setattr("worker.poller.process_task", fake_process_task)
    monkeypatch.setattr("worker.poller.persist_event_and_mark_consumed", lambda *a, **k: None)
    monkeypatch.setattr("worker.poller.mark_failed", lambda *a, **k: None)
    monkeypatch.setattr(time, "sleep", lambda s: None)

    def record_hb(ts, in_backoff=False, error=None):
        calls.append(in_backoff)

    db = FakeDB()
    poll_interval = 5
    worker_id = "test-worker"

    poller.run_iteration(db, worker_id, poll_interval, record_hb)
    poller.run_iteration(db, worker_id, poll_interval, record_hb)
    poller.run_iteration(db, worker_id, poll_interval, record_hb)

    assert calls == [False, True, False]
