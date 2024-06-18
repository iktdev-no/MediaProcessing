
from fuzzywuzzy import fuzz, process
from .AlgorithmBase import AlgorithmBase, MatchResult
from clazz.Metadata import Metadata


class SimpleMatcher(AlgorithmBase):
    def getBestMatch(self) -> Metadata | None:
        best_match = None
        best_score = -1
        match_results = []

        for title in self.titles:
            for metadata in self.metadata:
                # Match against metadata title
                score = fuzz.token_sort_ratio(title.lower(), metadata.title.lower())
                match_results.append(MatchResult(title, metadata.title, score, metadata.source, metadata))
                if score > best_score:
                    best_score = score
                    best_match = metadata if score >= 70 else None

                # Match against metadata altTitles
                for alt_title in metadata.altTitle:
                    alt_score = fuzz.token_sort_ratio(title.lower(), alt_title.lower())
                    match_results.append(MatchResult(title, alt_title, alt_score, metadata.source, metadata))
                    if alt_score > best_score:
                        best_score = alt_score
                        best_match = metadata if alt_score >= 70 else None

        # Print match summary
        self.print_match_summary(match_results)

        return best_match