# models/task.py
from pydantic import BaseModel
from uuid import UUID
from datetime import datetime
from typing import List, Optional
from models.enums import TaskStatus


class MetadataSearchData(BaseModel):
    searchTitles: List[str]
    collection: str


class Task(BaseModel):
    referenceId: UUID
    taskId: UUID
    task: str
    status: TaskStatus
    data: dict   # generisk payload hvis du ikke vet typen
    claimed: bool
    claimedBy: Optional[str]
    consumed: bool
    lastCheckIn: Optional[datetime]
    persistedAt: datetime


class MetadataSearchTask(Task):
    data: MetadataSearchData
