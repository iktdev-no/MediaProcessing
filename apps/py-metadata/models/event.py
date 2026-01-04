# models/event.py
from pydantic import BaseModel
from datetime import datetime
from typing import List, Set
from uuid import UUID

from models.enums import MediaType, TaskStatus


class EventMetadata(BaseModel):
    created: datetime
    derivedFromId: Set[UUID]   # nøyaktig feltnavn


class Summary(BaseModel):
    language: str
    description: str


class MetadataResult(BaseModel):
    source: str
    title: str
    alternateTitles: List[str]
    cover: str | None
    bannerImage: str | None    # behold camelCase
    type: MediaType
    summary: List[Summary]
    genres: List[str]


class SearchResult(BaseModel):
    simpleScore: int
    prefixScore: int
    advancedScore: int
    sourceWeight: float
    metadata: MetadataResult


class MetadataSearchResultEvent(BaseModel):
    # Påkrevde felter
    referenceId: UUID
    eventId: UUID
    metadata: EventMetadata

    # Custom felter
    results: List[SearchResult]
    recommended: SearchResult|None
    status: TaskStatus
