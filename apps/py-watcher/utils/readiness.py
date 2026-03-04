import os
import asyncio
from models.event import create_event, FileReadyEvent

async def file_is_ready(path: str, wait: float = 1.0) -> bool:
    try:
        size1 = os.path.getsize(path)
        await asyncio.sleep(wait)
        size2 = os.path.getsize(path)
        return size1 == size2
    except Exception:
        return False

async def check_ready(db, ref_id, file_name, file_uri, insert_event, derived_from_event_id):
    for _ in range(5):
        await asyncio.sleep(2)
        if await file_is_ready(file_uri):
            ev = create_event(
                FileReadyEvent,
                file_name,
                file_uri,
                reference_id=ref_id,
                derived_from={derived_from_event_id}
            )
            insert_event(db, ev)
            return ev
    return None
