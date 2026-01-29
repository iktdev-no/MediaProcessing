import signal
import sys
import time
from threading import Thread
import uvicorn

from api.health_api import app as health_app, init_health_api
from config.database_config import DatabaseConfig
from db.database import Database
from utils.logger import logger
from worker.poller import run_worker

shutdown_flag = False

# Heartbeat state
worker_heartbeat = time.time()
worker_in_backoff = False
worker_error = None


def handle_shutdown(signum, frame):
    global shutdown_flag
    logger.info("🛑 Shutdown signal mottatt, avslutter worker...")
    shutdown_flag = True


def set_heartbeat(ts, in_backoff=False, error=None):
    global worker_heartbeat, worker_in_backoff, worker_error
    worker_heartbeat = ts
    worker_in_backoff = in_backoff
    worker_error = error


def get_heartbeat():
    return {
        "ts": worker_heartbeat,
        "inBackoff": worker_in_backoff,
        "error": worker_error
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
