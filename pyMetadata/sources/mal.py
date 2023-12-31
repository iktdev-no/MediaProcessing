from mal import *
from .result import Metadata, DataResult

class metadata():
    name: str = None
    
    def __init__(self, name: str) -> None:
        self.name = name

    def lookup(self) -> DataResult:
        try:
            search = AnimeSearch(self.name)
            if (len(search.results) == 0):
                return DataResult(statusType="IGNORE", errorMessage="No results")
            anime = Anime(search.results[0].mal_id)
            meta = Metadata(
                title = anime.title,
                altTitle = [altName for altName in [anime.title_english, *anime.title_synonyms] if altName],
                cover = anime.image_url,
                summary = anime.synopsis,
                type = 'movie' if anime.type.lower() == 'movie' else 'serie',
                genres = anime.genres,
                source="mal",
                usedTitle=self.name
            )
            if (meta.title is None) or (meta.type is None):
                return DataResult("IGNORE", None, None)

            return DataResult("SUCCESS", None, meta)
        except Exception as e:
            return DataResult(statusType="ERROR", errorMessage=str(e))