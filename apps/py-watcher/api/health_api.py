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
        # Sjekk observers
        observers = observers_ref()
        observers_ok = all(obs.is_alive() for obs in observers)

        # Sjekk database
        db_ok = False
        try:
            db_ref().ping()
            db_ok = True
        except Exception:
            db_ok = False

        # Sjekk worker heartbeat
        worker_ok = False
        try:
            last = heartbeat_ref()
            worker_ok = (time.time() - last) < 10
        except Exception:
            worker_ok = False

        healthy = observers_ok and db_ok and worker_ok

        return JSONResponse(
            status_code=200 if healthy else 500,
            content={
                "status": "healthy" if healthy else "unhealthy",
                "observers": observers_ok,
                "database": db_ok,
                "worker": worker_ok
            }
        )

    return app
