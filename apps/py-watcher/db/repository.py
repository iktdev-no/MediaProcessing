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
    INSERT INTO EVENTS(REFERENCE_ID, EVENT_ID, EVENT, DATA, PERSISTED_AT)
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
    SELECT e.REFERENCE_ID, e.EVENT_ID, e.EVENT, e.DATA
    FROM EVENTS e
    WHERE e.EVENT = 'FileAddedEvent'
      AND NOT EXISTS (
          SELECT 1 FROM EVENTS r
          WHERE r.REFERENCE_ID = e.REFERENCE_ID
            AND r.EVENT IN ('FileReadyEvent', 'FileRemovedEvent')
      )
    ORDER BY e.PERSISTED_AT ASC
    """
    events: List[FileAddedEvent] = []
    with db.conn.cursor(dictionary=True) as cursor:
        cursor.execute(sql)
        rows = cursor.fetchall()
        for row in rows:
            # Bruk Pydantic v2 sin model_validate_json
            event = FileAddedEvent.model_validate_json(row["DATA"])
            # Overstyr referenceId og eventId fra kolonnene (sannhetskilde)
            event.referenceId = row["REFERENCE_ID"]
            event.eventId = row["EVENT_ID"]
            events.append(event)

    logger.info(f"🔎 Fant {len(events)} åpne FileAddedEvent uten Ready/Removed")
    return events