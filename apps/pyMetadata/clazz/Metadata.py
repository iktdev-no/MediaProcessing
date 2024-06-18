from dataclasses import asdict, dataclass
from typing import List, Optional

@dataclass
class Summary:
    summary: str
    language: str

    def to_dict(self):
        return asdict(self)


@dataclass
class Metadata:
    title: str
    altTitle: List[str]
    cover: str
    banner: Optional[str]
    type: str  # Serie/Movie
    summary: List[Summary]
    genres: List[str]
    source: str

    def to_dict(self):
        return asdict(self)
    
