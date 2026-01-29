from typing import Set
import pytest
from models.event import MetadataSearchResultEvent, EventMetadata
from worker.poller import run_worker, run_iteration
from models.task import MetadataSearchTask, MetadataSearchData
from models.enums import TaskStatus
import uuid
from utils.time import utc_now
import time


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
        data=MetadataSearchData(searchTitles=["foo"], collection="bar"),
        claimed=False,
        claimedBy=None,
        consumed=False,
        lastCheckIn=None,
        persistedAt=utc_now()
    )


def test_run_worker_processes_one(monkeypatch):
    events = []
    task = make_task()

    class FakeDB:
        def connect(self): pass
        def close(self): pass

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

    # NEW: dummy heartbeat
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

    class FakeDB:
        def connect(self): pass
        def close(self): pass

    monkeypatch.setattr("worker.poller.fetch_next_task", lambda db: None)

    def fake_sleep(seconds):
        intervals.append(seconds)

    monkeypatch.setattr(time, "sleep", fake_sleep)

    monkeypatch.setattr("worker.poller.claim_task", lambda db, tid, wid: True)
    monkeypatch.setattr("worker.poller.process_task", lambda t: "dummy_event")
    monkeypatch.setattr("worker.poller.persist_event_and_mark_consumed", lambda db, e, tid: None)
    monkeypatch.setattr("worker.poller.mark_failed", lambda db, tid: None)

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

    class FakeDB:
        def connect(self):
            reconnects.append("reconnect")
        def close(self): pass

    def failing_fetch(db):
        raise RuntimeError("DB connection lost")

    monkeypatch.setattr("worker.poller.fetch_next_task", failing_fetch)

    def fake_sleep(seconds):
        intervals.append(seconds)

    monkeypatch.setattr(time, "sleep", fake_sleep)

    monkeypatch.setattr("worker.poller.claim_task", lambda db, tid, wid: True)
    monkeypatch.setattr("worker.poller.process_task", lambda t: "dummy_event")
    monkeypatch.setattr("worker.poller.persist_event_and_mark_consumed", lambda db, e, tid: None)
    monkeypatch.setattr("worker.poller.mark_failed", lambda db, tid: None)

    dummy_hb = lambda ts, in_backoff=False, error=None: None

    run_worker(
        db=FakeDB(),
        shutdown_flag_ref=lambda: len(reconnects) >= 2,
        heartbeat_ref=dummy_hb
    )

    assert reconnects == ["reconnect", "reconnect"]
    assert all(interval == 5 for interval in intervals)
