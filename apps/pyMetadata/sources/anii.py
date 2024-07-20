import logging, sys
import hashlib
from typing import List, Dict, Optional

from clazz.Metadata import Metadata, Summary
from .source import SourceBase

from AnilistPython import Anilist
import asyncio

log = logging.getLogger(__name__)

class Anii(SourceBase):
    def __init__(self, titles: List[str]) -> None:
        super().__init__(titles)

    async def search(self) -> Optional[Metadata]:
        idToTitle: Dict[str, str] = {}
        results: Dict[str, Dict] = {}
        try:
            for title in self.titles:
                try:
                    result = await asyncio.to_thread(Anilist().get_anime, title)
                    if result:
                        _title = result.get("name_english", None)
                        if _title is None:
                            _title = result.get("name_romaji", None)
                        if _title is not None:
                            givenId = await asyncio.to_thread(self.generate_id, _title)
                            if givenId:
                                idToTitle[givenId] = _title
                                results[givenId] = result
                except IndexError:
                    pass
                except Exception as e:
                    log.exception(e)
        except IndexError:
            self.logNoMatch("Anii", titles=self.titles)
            pass
        except Exception as e:
            log.exception(e)

        if not idToTitle or not results:
            self.logNoMatch("Anii", titles=self.titles)
            return None

        best_match_id, best_match_title = await asyncio.to_thread(self.findBestMatchAcrossTitles, idToTitle, self.titles)

        return await self.__getMetadata(results[best_match_id])

    async def queryIds(self, title: str) -> Dict[str, str]:
        return await asyncio.to_thread(super().queryIds, title)

    async def __getMetadata(self, result: Dict) -> Optional[Metadata]:
        try:
            summary = result.get("desc", None)
            return Metadata(
                title=result.get("name_english", None),
                altTitle=[result.get("name_romaji", [])],
                cover=result.get("cover_image", None),
                banner=None,
                summary=[] if summary is None else [
                    Summary(
                        language="eng",
                        summary=summary
                    )
                ],
                type=self.getMediaType(result.get('airing_format', '')),
                genres=result.get('genres', []),
                source="anii",
            )
        except Exception as e:
            log.exception(e)
            return None

    def generate_id(self, text: str) -> Optional[str]:
        if text:
            return hashlib.md5(text.encode()).hexdigest()
        return None

    def getMediaType(self, type: str) -> str:
        return 'movie' if type.lower() == 'movie' else 'serie'