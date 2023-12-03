from typing import List, Optional
from dataclasses import dataclass, asdict

@dataclass
class Metadata:
    title: str
    altTitle: List[str]
    cover: str
    type: str  # Serie/Movie
    summary: str
    genres: List[str]
    source: str
    usedTitle: str

    def to_dict(self):
        return asdict(self)

@dataclass
class DataResult:
    status: str # COMPLETED / ERROR
    message: str | None = None
    data: Metadata = None

    def to_dict(self):
        return asdict(self)
