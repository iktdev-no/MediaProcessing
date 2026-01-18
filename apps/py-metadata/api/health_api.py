# health_api.py
import time
from fastapi import FastAPI
from fastapi.responses import JSONResponse

app = FastAPI()

db = None
get_worker_heartbeat = None


def init_health_api(database, heartbeat_ref):
    global db, get_worker_heartbeat
    db = database
    get_worker_heartbeat = heartbeat_ref


@app.get("/health")
async def health():
    db_ok = False
    worker_ok = False
    db_error = None
    worker_error = None

    # --- Database check ---
    try:
        db.ping()
        db_ok = True
    except Exception as e:
        db_ok = False
        db_error = str(e)

    # --- Worker heartbeat check ---
    try:
        last = get_worker_heartbeat()
        now = time.time()
        diff = now - last

        worker_ok = diff < 90 # 90 sekunder toleranse pga at worker kan være inaktiv ved lav belastning

        # Hvis worker er false og ingen exception ble kastet → legg diff i worker_error
        if not worker_ok and worker_error is None:
            worker_error = f"Heartbeat too old: {diff:.2f}s"

    except Exception as e:
        worker_ok = False
        worker_error = str(e)

    status = db_ok and worker_ok

    return JSONResponse(
        status_code=200 if status else 500,
        content={
            "status": "healthy" if status else "unhealthy",
            "database": db_ok,
            "database_error": db_error,
            "worker": worker_ok,
            "worker_error": worker_error
        }
    )
