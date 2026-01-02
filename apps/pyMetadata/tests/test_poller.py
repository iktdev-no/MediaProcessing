from typing import Set
import pytest
from models.event import MetadataSearchResultEvent, EventMetadata
from worker.poller import run_worker, run_iteration
from models.task import MetadataSearchTask, MetadataSearchData
from models.enums import TaskStatus
import uuid
from datetime import datetime
import time

def make_dummy_event():
    return MetadataSearchResultEvent(
        referenceId=uuid.uuid4(),
        eventId=uuid.uuid4(),
        metadata=EventMetadata(
            created=datetime.now(),
            derivedFromId={uuid.uuid4()}
        ),
        results=[],
        persistedAt=datetime.now(),
        recommended=None,              # fyll inn med en gyldig bool
        status="Completed"                # eller enum hvis modellen krever det
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
        persistedAt=datetime.now()
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

    # Viktig: async stub
    async def fake_process_task(task):
        return make_dummy_event()
    monkeypatch.setattr("worker.poller.process_task", fake_process_task)

    def persist_stub(db, event, task_id):
        events.append("dummy_event")
    monkeypatch.setattr("worker.poller.persist_event_and_mark_consumed", persist_stub)

    monkeypatch.setattr("worker.poller.mark_failed", lambda *a, **k: events.append("failed"))
    monkeypatch.setattr("worker.poller.time.sleep", lambda s: None)

    run_worker(db=FakeDB(), shutdown_flag_ref=lambda: calls["n"] >= 2)

    assert "dummy_event" in events




def test_backoff(monkeypatch):
    intervals = []

    class FakeDB:
        def connect(self): pass
        def close(self): pass

    # monkeypatch fetch_next_task til å returnere None flere ganger
    monkeypatch.setattr("worker.poller.fetch_next_task", lambda db: None)

    # monkeypatch time.sleep til å fange poll_interval
    def fake_sleep(seconds):
        intervals.append(seconds)
    monkeypatch.setattr(time, "sleep", fake_sleep)

    # monkeypatch claim_task, process_task osv. til dummy
    monkeypatch.setattr("worker.poller.claim_task", lambda db, tid, wid: True)
    monkeypatch.setattr("worker.poller.process_task", lambda t: "dummy_event")
    monkeypatch.setattr("worker.poller.persist_event_and_mark_consumed", lambda db, e, tid: None)
    monkeypatch.setattr("worker.poller.mark_failed", lambda db, tid: None)

    # kjør bare noen få iterasjoner ved å stoppe med shutdown_flag_ref
    run_worker(db=FakeDB(), shutdown_flag_ref=lambda: len(intervals) >= 4)

    # verifiser at intervallet øker (5 → 10 → 20 → 40)
    assert intervals == [5, 10, 20, 40]

def test_backoff_on_connection_error(monkeypatch):
    intervals = []
    reconnects = []

    class FakeDB:
        def connect(self):
            reconnects.append("reconnect")
        def close(self): pass

    # Først: fetch_next_task kaster exception
    def failing_fetch(db):
        raise RuntimeError("DB connection lost")

    monkeypatch.setattr("worker.poller.fetch_next_task", failing_fetch)

    # monkeypatch time.sleep til å fange poll_interval
    def fake_sleep(seconds):
        intervals.append(seconds)
    monkeypatch.setattr(time, "sleep", fake_sleep)

    # dummy funksjoner
    monkeypatch.setattr("worker.poller.claim_task", lambda db, tid, wid: True)
    monkeypatch.setattr("worker.poller.process_task", lambda t: "dummy_event")
    monkeypatch.setattr("worker.poller.persist_event_and_mark_consumed", lambda db, e, tid: None)
    monkeypatch.setattr("worker.poller.mark_failed", lambda db, tid: None)

    # kjør bare noen få iterasjoner
    run_worker(db=FakeDB(), shutdown_flag_ref=lambda: len(reconnects) >= 2)

    # verifiser at reconnect ble kalt
    assert reconnects == ["reconnect", "reconnect"]

    # verifiser at poll_interval ble reset til 5 etter feil
    assert all(interval == 5 for interval in intervals)
