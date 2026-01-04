
import logging
from typing import List
from fuzzywuzzy import fuzz, process
from .AlgorithmBase import AlgorithmBase, MatchResult
from models.metadata import Metadata


class SimpleMatcher(AlgorithmBase):
    def getScore(self) -> int:
        best_score = fuzz.token_sort_ratio(self.title.lower(), self.metadata.title.lower())

        for alt in self.metadata.altTitle:
            alt_score = fuzz.token_sort_ratio(self.title.lower(), alt.lower())
            best_score = max(best_score, alt_score)

        return best_score
