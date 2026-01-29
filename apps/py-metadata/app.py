import signal
import sys
import time
from datetime import datetime
from threading import Thread
import uvicorn

from api.health_api import app as health_app, init_health_api
from config.database_config import DatabaseConfig
from db.database import Database
from utils.logger import logger
from worker.poller import run_worker

shutdown_flag = False

# Heartbeat state (nå med full backoff-tracking)
worker_heartbeat = time.time()
worker_in_backoff = False
worker_error = None

backoff_entered_at = None
backoff_exited_at = None
backoff_entered_human = None
backoff_exited_human = None


def handle_shutdown(signum, frame):
    global shutdown_flag
    logger.info("🛑 Shutdown signal mottatt, avslutter worker...")
    shutdown_flag = True


def set_heartbeat(ts, in_backoff=False, error=None):
    """
    Oppdaterer worker-state, inkludert når backoff starter og slutter.
    """
    global worker_heartbeat, worker_in_backoff, worker_error
    global backoff_entered_at, backoff_exited_at
    global backoff_entered_human, backoff_exited_human

    prev: bool = worker_in_backoff

    # Går INN i backoff
    if in_backoff and prev is False:
        backoff_entered_at = ts
        backoff_entered_human = datetime.fromtimestamp(ts).isoformat()

    # Går UT av backoff
    if not in_backoff and prev is True:
        backoff_exited_at = ts
        backoff_exited_human = datetime.fromtimestamp(ts).isoformat()

    worker_heartbeat = ts
    worker_in_backoff = in_backoff
    worker_error = error


def get_heartbeat():
    """
    Returnerer all metadata health_api trenger.
    """
    now = time.time()

    # Beregn varighet i backoff
    if backoff_entered_at and not backoff_exited_at:
        duration = now - backoff_entered_at
    elif backoff_entered_at and backoff_exited_at:
        duration = backoff_exited_at - backoff_entered_at
    else:
        duration = None

    duration_human = f"{duration:.2f}s" if duration else None

    return {
        "ts": worker_heartbeat,
        "inBackoff": worker_in_backoff,
        "error": worker_error,
        "backoffEnteredAt": backoff_entered_at,
        "backoffExitedAt": backoff_exited_at,
        "backoffEnteredHuman": backoff_entered_human,
        "backoffExitedHuman": backoff_exited_human,
        "backoffDurationSeconds": duration,
        "backoffDurationHuman": duration_human,
    }


def start_health_server():
    uvicorn.run(health_app, host="0.0.0.0", port=8080, log_level="error")


def main():
    signal.signal(signal.SIGINT, handle_shutdown)
    signal.signal(signal.SIGTERM, handle_shutdown)

    logger.info("🚀 Starter worker-applikasjon")

    try:
        config = DatabaseConfig.from_env()
        db = Database(config)
        db.connect()

        # Init health API
        init_health_api(db, get_heartbeat)

        # Start health server i egen tråd
        Thread(target=start_health_server, daemon=True).start()
        logger.info("🌡️ Health API startet på port 8080")

        # Worker kjører i main-tråden
        logger.info("🔁 Starter worker i main-tråden")
        run_worker(
            db=db,
            shutdown_flag_ref=lambda: shutdown_flag,
            heartbeat_ref=set_heartbeat
        )

    except Exception as e:
        logger.error(f"❌ Kritisk feil i app: {e}")
        sys.exit(1)

    logger.info("👋 Worker avsluttet gracefully")


if __name__ == "__main__":
    main()
