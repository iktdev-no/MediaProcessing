
import logging, re
from abc import ABC, abstractmethod
from typing import List, Tuple

from fuzzywuzzy import fuzz

from clazz.Metadata import Metadata

log = logging.getLogger(__name__)

class SourceBase(ABC):
    titles: List[str] = []
    

    def __init__(self, titles: List[str]) -> None:
        self.titles = titles

    @abstractmethod
    def search(self, ) -> Metadata | None:
        pass

    @abstractmethod
    def queryIds(self, title: str) -> dict[str, str]:
        pass

    def isMatchOrPartial(self, source: str | None, title, foundTitle) -> bool:
        titleParts = re.split(r'[^a-zA-Z0-9\s]', foundTitle)
        clean_foundTitle: str | None = titleParts[0].strip() if titleParts else None
        directMatch = fuzz.ratio(title, foundTitle)
        partialMatch = fuzz.ratio(title, clean_foundTitle) if clean_foundTitle is not None else 0

        if directMatch >= 60:
            log.info(f"{source} -> Direct Match for '{title}' of '{foundTitle}' on part '{clean_foundTitle}' with direct score: {directMatch} and partial {partialMatch}")
            return True
        elif partialMatch >= 80:
            log.info(f"{source} -> Partial Match for '{title}' of '{foundTitle}' on part '{clean_foundTitle}' with direct score: {directMatch} and partial {partialMatch}")
            return True
        else:
            log.info(f"{source} -> Match failed for '{title}' of '{foundTitle}' on part '{clean_foundTitle}' with direct score: {directMatch} and partial {partialMatch}")
        return False


    def getMatchingOnTitleWords(self, idToTitle: dict[str, str], titles: List[str]) -> dict[str, str]:
        matched_idToTitle = {}

        for title in titles:
            title_words = set(title.split())
            for id, stored_title in idToTitle.items():
                stored_title_words = set(stored_title.split())
                if title_words & stored_title_words:  # sjekker om det er et felles ord
                    score = fuzz.token_set_ratio(title, stored_title)
                    if score >= 75:
                        matched_idToTitle[id] = (stored_title, score)
        
        # Returnerer den originale dict med score 0 hvis ingen titler matcher
        if not matched_idToTitle:
            for id, stored_title in idToTitle.items():
                matched_idToTitle[id] = (stored_title, 0)
        
        # Returnerer den originale dict hvis ingen titler matcher
        return matched_idToTitle if matched_idToTitle else idToTitle

    def findBestMatchAcrossTitles(self, idToTitle: dict[str, str], titles: List[str]) -> Tuple[str, str]:
        # Få den filtrerte eller originale idToTitle basert på ordmatching
        filtered_idToTitle = self.getMatchingOnTitleWords(idToTitle, titles)
        
        best_match_id = ""
        best_match_title = ""
        best_ratio = 0

        for title in titles:
            for id, stored_title in filtered_idToTitle.items():
                ratio = fuzz.ratio(title, stored_title)
                if ratio > best_ratio:
                    best_ratio = ratio
                    best_match_id = id
                    best_match_title = stored_title

        return best_match_id, best_match_title
    
    def logNoMatch(self, source: str, titles: List[str]) -> None:
        combined_titles = ", ".join(titles)
        log.info(f"No match in source {source} for titles: {combined_titles}")