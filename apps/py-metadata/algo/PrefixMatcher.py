import re
from typing import List, Optional
from fuzzywuzzy import fuzz, process
from .AlgorithmBase import AlgorithmBase, MatchResult
from models.metadata import Metadata


class PrefixMatcher(AlgorithmBase):
    def preprocess(self, s: str) -> str:
        return re.sub(r'[^a-zA-Z0-9\s]', ' ', s).strip().lower()

    def first_word(self, s: str) -> str:
        return self.preprocess(s).split(" ")[0] if s else ""

    def getScore(self) -> int:
        best_score = 0
        pt = self.first_word(self.title)

        # Mot hovedtittel
        meta_main = self.first_word(self.metadata.title)
        best_score = max(best_score, fuzz.ratio(pt, meta_main))

        # Mot altTitler
        for alt in self.metadata.altTitle:
            alt_score = fuzz.ratio(pt, self.first_word(alt))
            best_score = max(best_score, alt_score)

        return best_score
