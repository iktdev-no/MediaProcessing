import signal
import sys
from threading import Thread
from api.health_api import app as health_app, init_health_api
from config.database_config import DatabaseConfig
from db.database import Database
from utils.logger import logger
from worker.poller import run_worker
import uvicorn

# global flag for shutdown
shutdown_flag = False
worker_heartbeat = 0 

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
    """ Starter FastAPI health-server i egen tråd. """
    uvicorn.run(health_app, host="0.0.0.0", port=8080, log_level="warning")



def main():
    # registrer signal handlers for graceful shutdown
    signal.signal(signal.SIGINT, handle_shutdown)
    signal.signal(signal.SIGTERM, handle_shutdown)

    logger.info("🚀 Starter worker-applikasjon")
    try:
        config: DatabaseConfig = DatabaseConfig.from_env()
        db: Database = Database(config)
        db.connect()

        # Init health-API med DB og heartbeat-ref
        init_health_api(db, get_heartbeat)

        # Start health-server i egen tråd        
        Thread(target=start_health_server, daemon=True).start()
        logger.info("🌡️ Health API startet på port 8080")
        

        run_worker(db=db, shutdown_flag_ref=lambda: shutdown_flag, heartbeat_ref=lambda ts: set_heartbeat(ts))
    except Exception as e:
        logger.error(f"❌ Kritisk feil i app: {e}")
        sys.exit(1)

    logger.info("👋 Worker avsluttet gracefully")

if __name__ == "__main__":
    main()
