from fuzzywuzzy import fuzz
import re
from .AlgorithmBase import AlgorithmBase, MatchResult
from clazz.Metadata import Metadata

class AdvancedMatcher(AlgorithmBase):
    def clean_title(self, title: str) -> str:
        # Fjerner eventuelle ekstra tekster etter kolon eller andre skilletegn
        return re.sub(r'[:\-\—].*', '', title).strip()

    def getBestMatch(self) -> Metadata | None:
        best_match = None
        best_score = -1
        match_results = []

        for title in self.titles:
            cleaned_title = self.clean_title(title)  # Renset tittel uten ekstra tekst
            for metadata in self.metadata:
                cleaned_metadata_title = self.clean_title(metadata.title)  # Renset metadata-tittel

                # Compute different match ratios for both the original and cleaned titles
                original_title_ratio = fuzz.token_sort_ratio(title.lower(), metadata.title.lower())
                cleaned_title_ratio = fuzz.token_sort_ratio(cleaned_title.lower(), cleaned_metadata_title.lower())

                alt_title_ratios = [fuzz.token_sort_ratio(cleaned_title.lower(), self.clean_title(alt_title).lower()) for alt_title in metadata.altTitle]
                max_alt_title_ratio = max(alt_title_ratios) if alt_title_ratios else 0

                # Combine ratios: take the best of original vs cleaned title, and alt title match
                combined_score = max(original_title_ratio, cleaned_title_ratio, max_alt_title_ratio)

                match_results.append(MatchResult(title, metadata.title, combined_score, metadata.source, metadata))

                # Update best match if this one is better
                if combined_score > best_score:
                    best_score = combined_score
                    best_match = metadata if combined_score >= 70 else None

        # Print match summary
        self.print_match_summary(match_results)

        return best_match