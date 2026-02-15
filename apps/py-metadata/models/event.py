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
    # Hva brukeren faktisk søkte på (alle titler)
    searchTitles: List[str]

    # Dice similarity score (0–100)
    similarity: int

    # Weak prefix score (0–10)
    prefix: int

    # Keyword score (kan være negativ)
    keywordScore: float

    # Type match/mismatch score
    typeScore: float

    # Completeness score
    completenessScore: float

    # Source priority score
    sourceScore: float

    # Total score (float)
    totalScore: float

    # Metadata som ble matchet
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
