# api/health_api.py
import time
from fastapi import FastAPI
from fastapi.responses import JSONResponse

def create_health_app(observers_ref, db_ref, heartbeat_ref):
    """
    Returnerer en FastAPI-app med /health endpoint.
    observers_ref: lambda -> liste av observer-tråder
    db_ref: lambda -> Database-objekt
    heartbeat_ref: lambda -> siste worker heartbeat timestamp
    """
    app = FastAPI()

    @app.get("/health")
    def health():
        # --- Observer status ---
        observers = observers_ref()
        observers_ok = all(obs.is_alive() for obs in observers)
        observers_error = None
        if not observers_ok:
            dead = [type(obs).__name__ for obs in observers if not obs.is_alive()]
            observers_error = f"Dead observers: {dead}"

        # --- Database status ---
        db_ok = False
        db_error = None
        try:
            db_ref().ping()
            db_ok = True
        except Exception as e:
            db_ok = False
            db_error = str(e)

        # --- Worker heartbeat ---
        worker_ok = False
        worker_error = None
        try:
            last = heartbeat_ref()
            worker_ok = (time.time() - last) < 10
            if not worker_ok:
                worker_error = f"Heartbeat too old: {time.time() - last:.1f}s"
        except Exception as e:
            worker_ok = False
            worker_error = str(e)

        # --- Combined status ---
        healthy = observers_ok and db_ok and worker_ok

        return JSONResponse(
            status_code=200 if healthy else 500,
            content={
                "status": "healthy" if healthy else "unhealthy",
                "observers": observers_ok,
                "observers_error": observers_error,
                "database": db_ok,
                "database_error": db_error,
                "worker": worker_ok,
                "worker_error": worker_error
            }
        )

    return app
