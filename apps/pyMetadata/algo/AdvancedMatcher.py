import re
from typing import List
from fuzzywuzzy import fuzz
from models.metadata import Metadata
from .AlgorithmBase import AlgorithmBase, MatchResult

class AdvancedMatcher(AlgorithmBase):
    def clean(self, s: str) -> str:
        # Fjern alt etter kolon eller bindestrek, normaliser til lowercase
        return re.sub(r'[:\-\—].*', '', s).strip().lower()

    def getScore(self) -> int:
        best_score = 0

        cleaned_title = self.clean(self.title)
        cleaned_metadata_title = self.clean(self.metadata.title)

        # Sammenlign original
        best_score = max(best_score, fuzz.token_sort_ratio(self.title.lower(), self.metadata.title.lower()))

        # Sammenlign renset
        best_score = max(best_score, fuzz.token_sort_ratio(cleaned_title, cleaned_metadata_title))

        # Sammenlign mot altTitler
        for alt in self.metadata.altTitle:
            alt_score = fuzz.token_sort_ratio(cleaned_title, self.clean(alt))
            best_score = max(best_score, alt_score)

        return best_score