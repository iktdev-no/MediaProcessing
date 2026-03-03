import json
from models.event import create_event, FileAddedEvent
from db.repository import get_open_added_events

class FakeCursor:
    def __init__(self, rows):
        self.rows = rows
    def execute(self, sql, params=None): pass
    def fetchall(self): return self.rows
    def __enter__(self): return self
    def __exit__(self, *a): pass

class FakeConn:
    def cursor(self, dictionary=True): return self.cursor_obj
    def __init__(self, rows): self.cursor_obj = FakeCursor(rows)

class FakeDB:
    def __init__(self, rows): self.conn = FakeConn(rows)
    def validate(self): pass

def test_get_open_added_events_returns_typed_objects():
    ev = create_event(FileAddedEvent, "test.csv", "/tmp/test.csv", reference_id="ref123")
    row = {
        "REFERENCE_ID": ev.referenceId,  # Endret til uppercase for å matche case-sensitivitet
        "EVENT_ID": ev.eventId,          # Endret til uppercase for å matche case-sensitivitet
        "EVENT": "FileAddedEvent",
        "DATA": ev.model_dump_json()
    }
    db = FakeDB([row])
    events = get_open_added_events(db)
    assert isinstance(events[0], FileAddedEvent)
    assert events[0].data.fileName == "test.csv"
