import signal
import sys
from config.database_config import DatabaseConfig
from db.database import Database
from utils.logger import logger
from worker.poller import run_worker

# global flag for shutdown
shutdown_flag = False

def handle_shutdown(signum, frame):
    global shutdown_flag
    logger.info("🛑 Shutdown signal mottatt, avslutter worker...")
    shutdown_flag = True

def main():
    # registrer signal handlers for graceful shutdown
    signal.signal(signal.SIGINT, handle_shutdown)
    signal.signal(signal.SIGTERM, handle_shutdown)

    logger.info("🚀 Starter worker-applikasjon")
    try:
        config: DatabaseConfig = DatabaseConfig.from_env()
        db: Database = Database(config)
        db.connect()
        run_worker(db=db, shutdown_flag_ref=lambda: shutdown_flag)
    except Exception as e:
        logger.error(f"❌ Kritisk feil i app: {e}")
        sys.exit(1)

    logger.info("👋 Worker avsluttet gracefully")

if __name__ == "__main__":
    main()
