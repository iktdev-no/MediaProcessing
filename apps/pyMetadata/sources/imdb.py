import logging
from imdb import Cinemagoer
from imdb.Movie import Movie

from typing import List

from clazz.Metadata import Metadata, Summary
from .source import SourceBase


log = logging.getLogger(__name__)

class Imdb(SourceBase):

    def __init__(self, titles: List[str]) -> None:
        super().__init__(titles)

    def search(self) -> Metadata | None:
        idToTitle: dict[str, str] = {}
        for title in self.titles:
            receivedIds = self.queryIds(title)
            for id, title in receivedIds.items():
                idToTitle[id] = title

        if not idToTitle:
            self.logNoMatch("Imdb", titles=self.titles)
            return None

        best_match_id, best_match_title = self.findBestMatchAcrossTitles(idToTitle, self.titles)

        return self.__getMetadata(best_match_id)
    
    def queryIds(self, title: str) -> dict[str, str]:
        idToTitle: dict[str, str] = {}

        try:
            search = Cinemagoer().search_movie(title)
            cappedResult: List[Movie] = search[:5]
            usable: List[Movie] = [found for found in cappedResult if self.isMatchOrPartial("Imdb", title, found._getitem("title"))]
            for item in usable:
                idToTitle[item.movieID] = item._getitem("title")
        except Exception as e:
            log.exception(e)
        return idToTitle    

    def __getMetadata(self, id: str) -> Metadata | None:
        try:
            result = Cinemagoer().get_movie(id)
            summary = result.get("plot outline", None)
            return Metadata(
                title = result.get("title", None),
                altTitle = [result.get("localized title", [])],
                cover = result.get("cover url", None),
                banner = None,
                summary = [] if summary is None else [
                    Summary(
                        language = "eng",
                        summary = summary
                    )
                ],
                type = self.getMediaType(result.get('kind', '')),
                genres = result.get('genres', []),
                source="imdb",
            )
        except Exception as e:
            log.exception(e)
        return None
    
    def getMediaType(self, type: str) -> str:
        return 'movie' if type.lower() == 'movie' else 'serie'