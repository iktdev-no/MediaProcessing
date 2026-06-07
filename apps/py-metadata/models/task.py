from pydantic import BaseModel
from uuid import UUID
from datetime import datetime
from typing import List, Optional
from models.enums import MediaType, TaskStatus


class Metadata(BaseModel):
    created: datetime
    derivedFromId: List[UUID]


class MetadataSearchData(BaseModel):
    searchTitles: List[str]
    collection: str
    mediaType: MediaType


class MetadataSearchPayload(BaseModel):
    data: MetadataSearchData
    referenceId: UUID
    taskId: UUID
    metadata: Metadata


class Task(BaseModel):
    referenceId: UUID
    taskId: UUID
    task: str
    status: TaskStatus
    data: dict
    claimed: bool
    claimedBy: Optional[str]
    consumed: bool
    lastCheckIn: Optional[datetime]
    persistedAt: datetime


class MetadataSearchTask(Task):
    data: MetadataSearchData
    metadata: Metadata
