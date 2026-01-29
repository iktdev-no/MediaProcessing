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
    in_backoff = None

    # --- Database check ---
    try:
        db.ping()
        db_ok = True
    except Exception as e:
        db_ok = False
        db_error = str(e)

    # --- Worker heartbeat check ---
    try:
        hb = get_worker_heartbeat()
        last = hb["ts"]
        in_backoff = hb["inBackoff"]
        worker_error = hb["error"]

        now = time.time()
        diff = now - last

        # Worker må ha iterert siste 90 sekunder
        worker_ok = diff < 90 and worker_error is None

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
            "worker_error": worker_error,
            "worker_in_backoff": in_backoff
        }
    )
