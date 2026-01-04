from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import List
from fuzzywuzzy import fuzz
from tabulate import tabulate
from models.metadata import Metadata

@dataclass
class MatchResult:
    title: str
    matched_title: str
    score: int
    source: str
    data: Metadata

class AlgorithmBase(ABC):
    def __init__(self, title: str, metadata: Metadata):
        self.title = title
        self.metadata = metadata

    @abstractmethod
    def getScore(self) -> int:
        """
        Returnerer alle matchresultater med scorer.
        """
        pass

    def print_match_summary(self, match_results: List[MatchResult]) -> None:
        headers = ["Title", "Matched Title", "Score", "Source"]
        data = [(r.title, r.matched_title, r.score, r.source) for r in match_results]
        print(tabulate(data, headers=headers))
