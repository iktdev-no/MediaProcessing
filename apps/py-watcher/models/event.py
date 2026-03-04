import uuid
from datetime import datetime
from typing import Optional, Set
from pydantic import BaseModel

from utils.time import utc_now

# --- Metadata ---
class Metadata(BaseModel):
    created: datetime
    derivedFromId: Optional[Set[str]] = None

# --- FileInfo ---
class FileInfo(BaseModel):
    fileName: str
    fileUri: str

# --- Base Event ---
class Event(BaseModel):
    referenceId: str
    eventId: str
    metadata: Metadata

# --- Spesifikke events ---
class FileAddedEvent(Event):
    data: FileInfo

class FileReadyEvent(Event):
    data: FileInfo

class FileRemovedEvent(Event):
    data: FileInfo

class FileChangedEvent(Event):
    data: FileInfo


# --- Helper-funksjoner ---
def create_event(event_cls, file_name: str, file_uri: str, reference_id: Optional[str] = None, derived_from: Optional[Set[str]] = None) -> Event:
    return event_cls(
        referenceId=reference_id or str(uuid.uuid4()),
        eventId=str(uuid.uuid4()),
        metadata=Metadata(
            created=utc_now(),
            derivedFromId=derived_from
        ),
        data=FileInfo(fileName=file_name, fileUri=file_uri)
    )
