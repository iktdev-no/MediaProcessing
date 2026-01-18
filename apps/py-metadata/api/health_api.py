# health_api.py
import time
from fastapi import FastAPI
from fastapi.responses import JSONResponse

app = FastAPI()

# Disse settes av app.py
db = None
get_worker_heartbeat = None


def init_health_api(database, heartbeat_ref):
    """
    Kalles fra app.py for å gi health-API tilgang til DB og worker-heartbeat.
    """
    global db, get_worker_heartbeat
    db = database
    get_worker_heartbeat = heartbeat_ref


@app.get("/health")
async def health():
    db_ok = False
    worker_ok = False

    # Sjekk database
    try:
        db.ping()
        db_ok = True
    except Exception:
        db_ok = False

    # Sjekk worker heartbeat
    try:
        last = get_worker_heartbeat()
        worker_ok = (time.time() - last) < 10  # 10 sekunder uten heartbeat = død
    except Exception:
        worker_ok = False

    status = db_ok and worker_ok

    return JSONResponse(
        status_code=200 if status else 500,
        content={
            "status": "ok" if status else "error",
            "database": db_ok,
            "worker": worker_ok
        }
    )
