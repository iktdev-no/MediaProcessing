import re
from typing import List, Optional
from fuzzywuzzy import fuzz, process
from .AlgorithmBase import AlgorithmBase, MatchResult
from clazz.Metadata import Metadata


class PrefixMatcher(AlgorithmBase):

    def preprocess_text(self, text: str) -> str:
        unitext = re.sub(r'[^a-zA-Z0-9\s]', ' ', text)
        return unitext.strip().lower()
    
    def source_priority(self, source: str) -> int:
        priority_map = {'mal': 1, 'anii': 2, 'imdb': 3}
        return priority_map.get(source, 4)

    def getBestMatch(self) -> Optional[Metadata]:
        best_match = None
        best_score = -1
        match_results: List[MatchResult] = []

        for title in self.titles:
            preprocessed_title = self.preprocess_text(title)[:1]
            
            for metadata in self.metadata:
                preprocessed_metadata_title = self.preprocess_text(metadata.title)[:1]
                
                # Match against metadata title
                score = fuzz.token_sort_ratio(preprocessed_title, preprocessed_metadata_title)
                match_results.append(MatchResult(title, metadata.title, score, metadata.source, metadata))
                if score > best_score:
                    best_score = score
                    best_match = metadata if score >= 70 else None

                # Match against metadata altTitles
                for alt_title in metadata.altTitle:
                    preprocessed_alt_title = self.preprocess_text(alt_title)[:1]
                    alt_score = fuzz.token_sort_ratio(preprocessed_title, preprocessed_alt_title)
                    match_results.append(MatchResult(title, alt_title, alt_score, metadata.source, metadata))
                    if alt_score > best_score:
                        best_score = alt_score
                        best_match = metadata if alt_score >= 70 else None

        match_results.sort(key=lambda x: (-x.score, self.source_priority(x.source)))

        # Print match summary
        self.print_match_summary(match_results)

        if match_results:
            top_result = match_results[0].data
            return top_result

        return best_match
