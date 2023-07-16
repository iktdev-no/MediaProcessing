from AnilistPython import Anilist
from .result import Metadata, DataResult

class metadata():
    name: str = None
    anilist = Anilist()

    def __init__(self, name) -> None:
        self.name = name
    
    def lookup(self) -> DataResult:
        """"""
        try:
            result = self.anilist.get_anime(self.name)
            meta = Metadata()
            meta.title = result.get("name_english", None)
            meta.altTitle = result.get("name_romaji", None)
            meta.cover = result.get("cover_image", None)
            meta.summary = result.get("desc", None)

            airing_format = result.get('airing_format', '').lower()
            if airing_format == 'movie':
                meta.type = 'movie'
            else:
                meta.type = 'serie'
            meta.genres = result.get('genres', [])
            return DataResult("SUCCESS", None, meta)

        except IndexError as ingore:
            return DataResult(statusType="IGNORE", errorMessage=f"No result for {self.name}")
        except Exception as e:
            return DataResult(statusType="ERROR", errorMessage=str(e))
            