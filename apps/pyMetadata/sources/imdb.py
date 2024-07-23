import logging
from imdb import Cinemagoer
from imdb.Movie import Movie

from typing import List, Dict, Optional

from clazz.Metadata import Metadata, Summary
from .source import SourceBase

import asyncio

log = logging.getLogger(__name__)

class Imdb(SourceBase):
    def __init__(self, titles: List[str]) -> None:
        super().__init__(titles)

    async def search(self) -> Optional[Metadata]:
        idToTitle: Dict[str, str] = {}
        for title in self.titles:
            receivedIds = await self.queryIds(title)
            for id, title in receivedIds.items():
                idToTitle[id] = title

        if not idToTitle:
            self.logNoMatch("Imdb", titles=self.titles)
            return None

        best_match_id, best_match_title = self.findBestMatchAcrossTitles(idToTitle, self.titles)

        return await self.__getMetadata(best_match_id)
    
    async def queryIds(self, title: str) -> Dict[str, str]:
        idToTitle: Dict[str, str] = {}

        try:
            search = await asyncio.to_thread(Cinemagoer().search_movie, title)
            cappedResult: List[Movie] = search[:5]
            usable = [
                found for found in cappedResult if await asyncio.to_thread(self.isMatchOrPartial, "Imdb", title, found.get("title"))
            ]
            for item in usable:
                idToTitle[item.movieID] = item.get("title")
        except Exception as e:
            log.exception(e)
        return idToTitle    

    async def __getMetadata(self, id: str) -> Optional[Metadata]:
        try:
            result = await asyncio.to_thread(Cinemagoer().get_movie, id)
            cover = result.get_fullsizeURL()
            if cover is None or len(cover) == 0:
                cover = result.get("cover url", None)
            summary = result.get("plot outline", None)
            return Metadata(
                title=result.get("title", None),
                altTitle=[result.get("localized title", [])],
                cover=cover,
                banner=None,
                summary=[] if summary is None else [
                    Summary(
                        language="eng",
                        summary=summary
                    )
                ],
                type=self.getMediaType(result.get('kind', '')),
                genres=result.get('genres', []),
                source="imdb",
            )
        except Exception as e:
            log.exception(e)
        return None
    
    def getMediaType(self, type: str) -> str:
        return 'movie' if type.lower() == 'movie' else 'serie'