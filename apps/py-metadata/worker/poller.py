import asyncio
import time
import uuid
from typing import Optional

from db.database import Database
from db.repository import (
    claim_task,
    fetch_next_task,
    mark_failed,
    persist_event_and_mark_consumed
)
from models.event import MetadataSearchResultEvent
from worker.processor import process_task
from utils.logger import logger
from models.task import MetadataSearchTask, Task


def run_iteration(db: Database, worker_id: str, poll_interval: int, heartbeat_ref) -> tuple[int, int]:
    try:
        task: Optional[Task] = fetch_next_task(db)

        if task:
            heartbeat_ref(time.time(), in_backoff=False, error=None)

            if not isinstance(task, MetadataSearchTask):
                logger.warning(f"⚠️ Ukjent task-type {type(task)} for {task.taskId}, hopper over.")
                return poll_interval, poll_interval

            if not claim_task(db, str(task.taskId), worker_id):
                logger.info(f"⏩ Task {task.taskId} ble claimet av en annen worker.")
                return poll_interval, poll_interval

            logger.info(f"🔔 Fant task {task.taskId} ({task.task}), claimed by {worker_id}")

            try:
                event: MetadataSearchResultEvent = asyncio.run(process_task(db, task))
                if event:
                    persist_event_and_mark_consumed(db, event, str(task.taskId))
                    logger.info(f"✅ Task {task.taskId} ferdig prosessert")
                else:
                    raise RuntimeError("process_task returned nothing!")

            except Exception as task_error:
                logger.error(f"❌ Task {task.taskId} feilet: {task_error}")
                mark_failed(db, str(task.taskId))
                heartbeat_ref(time.time(), in_backoff=False, error=str(task_error))

            return poll_interval, 5

        else:
            logger.debug("Ingen nye tasks.")
            heartbeat_ref(time.time(), in_backoff=True, error=None)
            return poll_interval, min(poll_interval * 2, 60)

    except Exception as e:
        logger.error(f"⚠️ Feil i worker: {e}")
        db.connect()
        heartbeat_ref(time.time(), in_backoff=True, error=str(e))
        return poll_interval, 5


def run_worker(db: Database, shutdown_flag_ref=lambda: False, heartbeat_ref=None) -> None:
    poll_interval: int = 5
    worker_id = f"PyMetadata-{uuid.uuid4()}"

    while not shutdown_flag_ref():
        sleep_interval, poll_interval = run_iteration(db, worker_id, poll_interval, heartbeat_ref)
        time.sleep(sleep_interval)

    logger.info("👋 run_worker loop avsluttet")
    db.close()
