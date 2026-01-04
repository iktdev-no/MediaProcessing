from datetime import datetime
import json
from typing import List, Optional
from db.database import Database
from models.event import FileAddedEvent
from utils.logger import logger
from models.event import Event, FileAddedEvent

def insert_event(db: Database, event: Event) -> None:
    """Persistér et Event til Events-tabellen."""
    db.validate()
    sql = """
    INSERT INTO Events(reference_id, event_id, event, data, persisted_at)
    VALUES (%s, %s, %s, %s, NOW())
    """
    with db.conn.cursor() as cursor:
        cursor.execute(
            sql,
            (event.referenceId, event.eventId, event.__class__.__name__, event.model_dump_json())
        )
        db.conn.commit()
    logger.info(f"📦 Event persisted: {event.__class__.__name__} ({event.referenceId})")

def get_open_added_events(db: Database) -> List[FileAddedEvent]:
    """
    Hent alle FileAddedEvent som fortsatt er 'åpne',
    dvs. ikke har en FileReadyEvent eller FileRemovedEvent.
    Returnerer en liste med FileAddedEvent-objekter.
    """
    db.validate()
    sql = """
    SELECT e.reference_id, e.event_id, e.event, e.data
    FROM Events e
    WHERE e.event = 'FileAddedEvent'
      AND NOT EXISTS (
          SELECT 1 FROM Events r
          WHERE r.reference_id = e.reference_id
            AND r.event IN ('FileReadyEvent', 'FileRemovedEvent')
      )
    ORDER BY e.persisted_at ASC
    """
    events: List[FileAddedEvent] = []
    with db.conn.cursor(dictionary=True) as cursor:
        cursor.execute(sql)
        rows = cursor.fetchall()
        for row in rows:
            # Bruk Pydantic v2 sin model_validate_json
            event = FileAddedEvent.model_validate_json(row["data"])
            # Overstyr referenceId og eventId fra kolonnene (sannhetskilde)
            event.referenceId = row["reference_id"]
            event.eventId = row["event_id"]
            events.append(event)

    logger.info(f"🔎 Fant {len(events)} åpne FileAddedEvent uten Ready/Removed")
    return events