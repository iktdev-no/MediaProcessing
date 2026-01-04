import asyncio
import pytest
from app import run_worker

class FakeObserver:
    def __init__(self): self.stopped = False
    def stop(self): self.stopped = True
    def join(self, timeout=None): pass

@pytest.mark.asyncio
async def test_run_worker_stops_on_shutdown(monkeypatch):
    fake = FakeObserver()

    def shutdown_ref(): return True

    # monkeypatch start_observer as imported in app.py
    monkeypatch.setattr("app.start_observer", lambda *a, **kw: fake)

    await run_worker(db=object(), paths=["/tmp"], extensions={".csv"}, shutdown_flag_ref=shutdown_ref)
    assert fake.stopped   # nå blir True
