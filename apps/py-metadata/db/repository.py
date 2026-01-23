import json
from typing import Optional
from uuid import UUID
from db.database import Database
from models.enums import TaskStatus
from models.event import MetadataSearchResultEvent
from models.task import MetadataSearchPayload, MetadataSearchTask
from utils.logger import logger
from utils.time import parse_mysql_ts, utc_now

def fetch_next_task(db: Database) -> Optional[MetadataSearchTask]:
    db.validate()
    cursor = db.conn.cursor(dictionary=True)
    cursor.execute(
        "SELECT * FROM TASKS "
        "WHERE TASK='MetadataSearchTask' AND STATUS='Pending' AND CLAIMED=0 AND CONSUMED=0 "
        "ORDER BY PERSISTED_AT ASC LIMIT 1"
    )
    row = cursor.fetchone()
    if not row:
        return None

    try:
        # 1. Parse payload (JSON → Pydantic)
        payload = MetadataSearchPayload.model_validate_json(row["DATA"])

        # 2. Parse UUIDs (DB gir str → modellen krever UUID)
        reference_id = UUID(row["REFERENCE_ID"])
        task_id = UUID(row["TASK_ID"])

        # 3. Parse enum (DB gir str → modellen krever TaskStatus)
        status = TaskStatus(row["STATUS"])

        # 4. Parse timestamps (DB gir str → modellen krever datetime)
        last_check_in = parse_mysql_ts(row["LAST_CHECK_IN"])
        persisted_at = parse_mysql_ts(row["PERSISTED_AT"])

        # 5. Bygg modellen med riktige typer
        return MetadataSearchTask(
            referenceId=reference_id,
            taskId=task_id,
            task=row["TASK"],
            status=status,
            data=payload.data,
            claimed=row["CLAIMED"],
            claimedBy=row["CLAIMED_BY"],
            consumed=row["CONSUMED"],
            lastCheckIn=last_check_in,
            persistedAt=persisted_at
        )

    except Exception:
        logger.exception(f"❌ Feil ved deserialisering av task {row.get('TASK_ID')}")
        mark_failed(db, row["TASK_ID"])
        return None



def mark_failed(db: Database, task_id: str) -> None:
    cursor = db.conn.cursor()
    cursor.execute(
        "UPDATE TASKS SET STATUS='Failed' WHERE TASK_ID=%s",
        (task_id,)
    )
    db.conn.commit()

def claim_task(db: Database, task_id: str, worker_id: str) -> bool:
    """
    Marker en task som claimed av en gitt worker.
    Returnerer True hvis claim lykkes, False hvis task allerede er claimed.
    """
    db.validate()
    try:
        cursor = db.conn.cursor()
        # Oppdater bare hvis task ikke allerede er claimed
        cursor.execute(
            """
            UPDATE TASKS
            SET CLAIMED=1, CLAIMED_BY=%s, LAST_CHECK_IN=%s
            WHERE TASK_ID=%s AND CLAIMED=0 AND CONSUMED=0
            """,
            (worker_id, utc_now(), task_id)
        )
        db.conn.commit()
        return cursor.rowcount > 0
    except Exception as e:
        db.conn.rollback()
        raise RuntimeError(f"Claim feilet: {e}")



def persist_event_and_mark_consumed(db: Database, event: MetadataSearchResultEvent, task_id: str) -> None:
    """
    Persisterer et event og markerer tilhørende task som consumed i én transaksjon.
    Ruller tilbake hvis noe feiler.
    """
    db.validate()
    try:
        cursor = db.conn.cursor()

        # 1. Insert event
        as_data = event.model_dump_json()  # Pydantic v2
        event_name = event.__class__.__name__

        cursor.execute(
            """
            INSERT INTO EVENTS (REFERENCE_ID, EVENT_ID, EVENT, DATA, PERSISTED_AT)
            VALUES (%s, %s, %s, %s, %s)
            """,
            (
                str(event.referenceId),
                str(event.eventId),
                event_name,
                as_data,
                utc_now()
            )
        )

        # 2. Update task status
        cursor.execute(
            "UPDATE TASKS SET STATUS=%s, CONSUMED=1 WHERE TASK_ID=%s",
            (TaskStatus.COMPLETED.value, task_id)
        )

        # 3. Commit begge operasjoner
        db.conn.commit()

    except Exception as e:
        # Rull tilbake hvis noe feiler
        db.conn.rollback()
        raise RuntimeError(f"Transaksjon feilet: {e}")



