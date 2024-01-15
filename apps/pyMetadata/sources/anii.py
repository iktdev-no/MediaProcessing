from AnilistPython import Anilist
from .result import Metadata, DataResult, Summary

class metadata():
    name: str = None
    anilist = Anilist()

    def __init__(self, name) -> None:
        self.name = name
    
    def lookup(self) -> DataResult:
        """"""
        try:
            result = self.anilist.get_anime(self.name)

            meta = Metadata(
                title = result.get("name_english", None),
                altTitle = [result.get("name_romaji", [])],
                cover = result.get("cover_image", None),
                summary = [
                    Summary(
                        language = "eng",
                        summary = result.get("desc", None)
                    )
                ],
                type = 'movie' if result.get('airing_format', '').lower() == 'movie' else 'serie',
                genres = result.get('genres', []),
                source="anii",
                usedTitle=self.name
            )
            if (meta.title is None) or (meta.type is None):
                return DataResult(status="COMPLETED", message= None, data= None)

            return DataResult(status="COMPLETED", message= None, data=meta)

        except IndexError as ingore:
            return DataResult(status="COMPLETED", message=f"No result for {self.name}")
        except Exception as e:
            return DataResult(status="ERROR", message=str(e))
            