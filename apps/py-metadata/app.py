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
worker_heartbeat = time.time()


def handle_shutdown(signum, frame):
    global shutdown_flag
    logger.info("🛑 Shutdown signal mottatt, avslutter worker...")
    shutdown_flag = True


def set_heartbeat(ts):
    global worker_heartbeat
    worker_heartbeat = ts


def get_heartbeat():
    return worker_heartbeat


def start_health_server():
    uvicorn.run(health_app, host="0.0.0.0", port=8080, log_level="warning")


def start_worker(db):
    run_worker(
        db=db,
        shutdown_flag_ref=lambda: shutdown_flag,
        heartbeat_ref=lambda ts: set_heartbeat(ts)
    )


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

        # Start worker i egen tråd
        Thread(target=start_worker, args=(db,), daemon=True).start()
        logger.info("🔁 Worker startet i egen tråd")

        # Hold main-tråden i live
        while not shutdown_flag:
            time.sleep(1)

    except Exception as e:
        logger.error(f"❌ Kritisk feil i app: {e}")
        sys.exit(1)

    logger.info("👋 Worker avsluttet gracefully")


if __name__ == "__main__":
    main()
