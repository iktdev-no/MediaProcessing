import logging, sys
from typing import List

from clazz.Metadata import Metadata, Summary
from .source import SourceBase

from mal import Anime, AnimeSearch, AnimeSearchResult


log = logging.getLogger(__name__)


class Mal(SourceBase):
    """"""
    def __init__(self, titles: List[str]) -> None:
        super().__init__(titles)

    def search(self) -> Metadata | None:
        idToTitle: dict[str, str] = {}

        for title in self.titles:
            receivedIds = self.queryIds(title)
            for id, title in receivedIds.items():
                idToTitle[id] = title
        
        if not idToTitle:
            self.logNoMatch("MAL", titles=self.titles)
            return None

        best_match_id, best_match_title = self.findBestMatchAcrossTitles(idToTitle, self.titles)

        return self.__getMetadata(best_match_id)
    
    def queryIds(self, title: str) -> dict[str, str]:
        idToTitle: dict[str, str] = {}

        try:
            search = AnimeSearch(title)
            cappedResult: List[AnimeSearchResult] = search.results[:5]
            usable: List[AnimeSearchResult] = [found for found in cappedResult if self.isMatchOrPartial("MAL", title, found.title)]
            for item in usable:
                log.info(f"malId: {item.mal_id} to {item.title}")
                idToTitle[item.mal_id] = item.title
        except Exception as e:
            log.exception(e)
        return idToTitle
    
    def __getMetadata(self, id: str):
        try:
            anime = Anime(id)
            return Metadata(
                    title = anime.title,
                    altTitle = [altName for altName in [anime.title_english, *anime.title_synonyms] if altName],
                    cover = anime.image_url,
                    banner = None,
                    summary = [] if anime.synopsis is None else [
                        Summary(
                            language = "eng",
                            summary = anime.synopsis
                        )
                    ],
                    type = self.getMediaType(anime.type),
                    genres = anime.genres,
                    source="mal",
                )
        except Exception as e:
            log.exception(e)
        return None
        
    def getMediaType(self, type: str) -> str:
        return 'movie' if type.lower() == 'movie' else 'serie'