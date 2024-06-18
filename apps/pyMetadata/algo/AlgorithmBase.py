

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import List

from fuzzywuzzy import fuzz, process
from tabulate import tabulate

from clazz.Metadata import Metadata

@dataclass
class MatchResult:
    title: str
    matched_title: str
    score: int
    source: str
    data: Metadata


class AlgorithmBase(ABC):
    def __init__(self, titles: List[str], metadata: List[Metadata]):
        self.titles = titles
        self.metadata = metadata

    @abstractmethod
    def getBestMatch(self) -> Metadata | None:
        pass

    def print_match_summary(self, match_results: List[MatchResult]):
        headers = ["Title", "Matched Title", "Score", "Source"]
        data = [(result.title, result.matched_title, result.score, result.source) for result in match_results]
        print(tabulate(data, headers=headers))