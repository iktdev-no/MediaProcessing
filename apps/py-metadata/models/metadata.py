from dataclasses import dataclass, asdict
from typing import List, Optional
from enum import Enum

from models.enums import MediaType


@dataclass
class Summary:
    summary: str
    language: str

    def to_dict(self):
        return {k: v.strip() if isinstance(v, str) else v for k, v in asdict(self).items()}

@dataclass
class Metadata:
    sourceId: int
    title: str
    altTitle: List[str]
    cover: str
    bannerImage: Optional[str]
    type: MediaType
    summary: List[Summary]
    genres: List[str]
    source: str

    def to_dict(self):
        def trim(item):
            if isinstance(item, str):
                return item.strip()
            elif isinstance(item, list):
                return [trim(sub_item) for sub_item in item]
            elif isinstance(item, Enum):
                return item.value
            elif hasattr(item, "to_dict"):
                return item.to_dict()
            return item

        return {key: trim(value) for key, value in asdict(self).items()}
