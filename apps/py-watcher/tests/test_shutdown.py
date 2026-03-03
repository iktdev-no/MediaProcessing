import asyncio
import pytest
from app import run_worker

class FakeObserver:
    def __init__(self):
        self.stopped = False
    def stop(self):
        self.stopped = True
    def join(self, timeout=None):
        pass

@pytest.mark.asyncio
async def test_run_worker_stops_on_shutdown(monkeypatch):
    fake = FakeObserver()

    def shutdown_ref():
        return True

    # mock start_observer slik det er importert i app.py
    monkeypatch.setattr("app.start_observer", lambda *a, **kw: fake)

    # lag en dummy event-loop for testen
    loop = asyncio.get_event_loop()

    await run_worker(
        db=object(),
        paths=["/tmp"],
        extensions={".csv"},
        shutdown_flag_ref=shutdown_ref,
        loop=loop
    )

    assert fake.stopped
