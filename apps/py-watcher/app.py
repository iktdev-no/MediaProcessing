import asyncio
import signal
import sys
import time
from threading import Thread
import uvicorn

from api.health_api import create_health_app
from config.database_config import DatabaseConfig
from db.database import Database
from db.repository import insert_event
from worker.file_watcher import start_observer
from utils.logger import logger

shutdown_flag = False
observers = []
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


async def run_worker(db: Database, paths, extensions, shutdown_flag_ref, loop):
    global observers
    observers = [
        start_observer(db, p, extensions, insert_event, loop)
        for p in paths
    ]



    try:
        while not shutdown_flag_ref():
            set_heartbeat(time.time())
            logger.debug("Heartbeat oppdatert")
            await asyncio.sleep(5)
    finally:
        logger.info("🛑 Stopper observer...")
        for obs in observers:
            obs.stop()
            obs.join()
        logger.info("👋 Alle observers stoppet")


def start_health_server(app):
    uvicorn.run(app, host="0.0.0.0", port=8080, log_level="error")


def main():
    signal.signal(signal.SIGINT, handle_shutdown)
    signal.signal(signal.SIGTERM, handle_shutdown)

    logger.info("🚀 Starter watcher-applikasjon")

    try:
        config = DatabaseConfig.from_env()
        db = Database(config)
        db.connect()

        from config.paths_config import PathsConfig
        paths_config = PathsConfig.from_env()
        paths_config.validate()

        # health API
        app = create_health_app(
            observers_ref=lambda: observers,
            db_ref=lambda: db,
            heartbeat_ref=get_heartbeat
        )

        # start health server i egen tråd
        Thread(target=start_health_server, args=(app,), daemon=True).start()
        logger.info("🌡️ Health API startet på port 8080")

        # start worker i event loop
        loop = asyncio.get_event_loop()
        loop.run_until_complete(run_worker(
            db,
            paths_config.watch_paths,
            paths_config.extensions,
            lambda: shutdown_flag,
            loop
        ))

    except Exception as e:
        logger.error(f"❌ Kritisk feil i app: {e}")
        sys.exit(1)

    logger.info("👋 Worker avsluttet gracefully")


if __name__ == "__main__":
    main()
