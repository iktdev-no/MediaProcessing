from fastapi import FastAPI
from fastapi.responses import JSONResponse

def create_health_app(observers_ref):
    """
    Returnerer en FastAPI-app med /health endpoint.
    observers_ref: en funksjon eller lambda som gir listen av observers.
    """
    app = FastAPI()

    @app.get("/health")
    def health():
        observers = observers_ref()
        healthy = all(obs.is_alive() for obs in observers)
        status = "healthy" if healthy else "unhealthy"
        code = 200 if healthy else 500
        return JSONResponse({"status": status}, status_code=code)

    return app
