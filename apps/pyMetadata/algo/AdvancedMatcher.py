from fuzzywuzzy import fuzz
from .AlgorithmBase import AlgorithmBase, MatchResult
from clazz.Metadata import Metadata

class AdvancedMatcher(AlgorithmBase):
    def getBestMatch(self) -> Metadata | None:
        best_match = None
        best_score = -1
        match_results = []

        for title in self.titles:
            for metadata in self.metadata:
                # Compute different match ratios
                title_ratio = fuzz.token_sort_ratio(title.lower(), metadata.title.lower())
                alt_title_ratios = [fuzz.token_sort_ratio(title.lower(), alt_title.lower()) for alt_title in metadata.altTitle]
                max_alt_title_ratio = max(alt_title_ratios) if alt_title_ratios else 0

                # Combine ratios as desired
                combined_score = max(title_ratio, max_alt_title_ratio)

                match_results.append(MatchResult(title, metadata.title, combined_score, metadata.source, metadata))

                # Update best match if this one is better
                if combined_score > best_score:
                    best_score = combined_score
                    best_match = metadata if combined_score >= 70 else None

        # Print match summary
        self.print_match_summary(match_results)

        return best_match
