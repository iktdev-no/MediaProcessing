from typing import List, Optional
from dataclasses import dataclass, asdict, field

@dataclass
class Metadata:
    title: str
    altTitle: List[str] = field(default_factory=list)
    cover: str
    type: str  # Serie/Movie
    summary: str
    genres: List[str] = field(default_factory=list)

    def to_dict(self):
        return asdict(self)

@dataclass
class DataResult:
    statusType: str
    errorMessage: str
    data: Metadata = None

    def to_dict(self):
        return asdict(self)

    @classmethod
    def from_dict(cls, data_dict):
        metadata_dict = data_dict.get('data')
        metadata = Metadata(**metadata_dict) if metadata_dict else None
        return cls(
            statusType=data_dict['statusType'],
            errorMessage=data_dict['errorMessage'],
            data=metadata
        )
