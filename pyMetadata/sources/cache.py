from typing import Optional
from .result import DataResult


class ResultCache:
    _cache = {}

    @classmethod
    def add(cls, title: str, result: DataResult):
        cls._cache[title] = result
    
    @classmethod
    def get(cls, title) -> Optional[DataResult]:
        return cls._cache.get(title)