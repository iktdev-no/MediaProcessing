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


# -------------------------------------------------------------------
# Tests
# -------------------------------------------------------------------

def test_persist_event_and_mark_consumed_success():
    rows_map = {}  # not used for writes
    db = FakeDB(rows_map)

    event = make_event()
    persist_event_and_mark_consumed(db, event, str(event.eventId))

    # verify commit
    assert db.conn.committed

    # verify two SQL statements executed
    assert len(db.conn.cursor().executed) == 2


def test_fetch_next_task_maps_correctly():
    task_id = uuid.uuid4()
    ref_id = uuid.uuid4()
    fake_row = make_task_row(task_id, ref_id)

    # Fake DB that returns one row for SELECT
    rows_map = {
        "TASKS": [fake_row]
    }

    db = FakeDB(rows_map)

    # Monkeypatch repository.execute_query to use FakeDB rows
    def fake_execute_query(db, sql, params=None):
        return rows_map["TASKS"]

    repository.execute_query = fake_execute_query

    task = repository.fetch_next_task(db)

    assert isinstance(task, MetadataSearchTask)
    assert task.taskId == task_id
    assert task.referenceId == ref_id
    assert task.status == TaskStatus.PENDING
    assert task.data.collection == "anime"
    assert task.data.searchTitles == ["Foo", "Bar"]
    assert task.data.mediaType == MediaType.MOVIE
    assert task.claimed is False
    assert task.consumed is False


# -------------------------------------------------------------------
# Unified Fake DB infrastructure (works for all repository tests)
# -------------------------------------------------------------------

# tests/fakes/fake_db.py

class FakeCursor:
    def __init__(self, conn):
        self.conn = conn
        self.executed = []

    def execute(self, sql, params=None):
        self.executed.append((sql, params))
        sql_upper = sql.upper()

        # Metadata tables
        if "FROM METADATA_TITLES" in sql_upper:
            self.conn.last_query = "METADATA_TITLES"
        elif "FROM METADATA_SUMMARIES" in sql_upper:
            self.conn.last_query = "METADATA_SUMMARIES"
        elif "FROM METADATA_GENRES" in sql_upper:
            self.conn.last_query = "METADATA_GENRES"
        elif "FROM METADATA" in sql_upper:
            self.conn.last_query = "METADATA"

        # Tasks
        elif "FROM TASKS" in sql_upper:
            self.conn.last_query = "TASKS"

    def fetchone(self):
        rows = self.conn.rows_map.get(self.conn.last_query, [])
        return rows[0] if rows else None

    def fetchall(self):
        return self.conn.rows_map.get(self.conn.last_query, [])

    def close(self):
        pass


class FakeConn:
    def __init__(self, rows_map=None):
        self.rows_map = rows_map or {}
        self.last_query = None
        self.committed = False
        self.rolled_back = False
        self._cursor = FakeCursor(self)

    def cursor(self, dictionary=False):
        return self._cursor

    def commit(self):
        self.committed = True

    def rollback(self):
        self.rolled_back = True


class FakeDB:
    """
    Fully compatible fake DB for:
    - metadata_repository
    - task_repository
    - search_runner
    - poller
    - persist_event_and_mark_consumed
    """

    def __init__(self, rows_map=None):
        self.conn = FakeConn(rows_map)

    # Poller expects these:
    def connect(self):
        pass

    def close(self):
        pass

    # search_runner expects this:
    def validate(self):
        pass
