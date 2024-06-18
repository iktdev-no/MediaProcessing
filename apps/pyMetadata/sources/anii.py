import logging, sys
import hashlib
from typing import List

from clazz.Metadata import Metadata, Summary
from .source import SourceBase

from AnilistPython import Anilist

log = logging.getLogger(__name__)

class Anii(SourceBase):

    def __init__(self, titles: List[str]) -> None:
        super().__init__(titles)

    def search(self) -> Metadata | None:
        idToTitle: dict[str, str] = {}
        results: dict[str, str] = {}
        try:
            for title in self.titles:
                try:
                    result = Anilist().get_anime(title)
                    if result:
                        _title = result.get("name_english", None)
                        givenId = self.generate_id(_title)
                        idToTitle[givenId] = _title
                        results[givenId] = result
                except IndexError as notFound:
                    pass
                except Exception as e:
                    log.exception(e)
        except IndexError as notFound:
            self.logNoMatch("Anii", titles=self.titles)
            pass
        except Exception as e:
            log.exception(e)


        if not idToTitle or not results:
            self.logNoMatch("Anii", titles=self.titles)
            return None
        
        best_match_id, best_match_title = self.findBestMatchAcrossTitles(idToTitle, self.titles)

        return self.__getMetadata(results[best_match_id])
    
    def queryIds(self, title: str) -> dict[str, str]:
        return super().queryIds(title)


    def __getMetadata(self, result: dict) -> Metadata:
        try:
            summary = result.get("desc", None)
            return Metadata(
                title = result.get("name_english", None),
                altTitle = [result.get("name_romaji", [])],
                cover = result.get("cover_image", None),
                banner = None,
                summary = [] if summary is None else [
                    Summary(
                        language = "eng",
                        summary = summary
                    )
                ],
                type = self.getMediaType(result.get('airing_format', '')),
                genres = result.get('genres', []),
                source="anii",
            )
        except Exception as e:
            log.exception(e)
            return None


    def generate_id(self, text: str):
        return hashlib.md5(text.encode()).hexdigest()            
    
    def getMediaType(self, type: str) -> str:
        return 'movie' if type.lower() == 'movie' else 'serie'
        



