import imdb
from .result import Metadata, DataResult

class metadata():
    name: str = None
    imdb = imdb.Cinemagoer()

    def __init__(self, name) -> None:
        self.name = name

    
    def lookup(self) -> DataResult:
        """"""
        try:
            query = self.imdb.search_movie(self.name)
            imdbId = query[0].movieID
            result = self.imdb.get_movie(imdbId)
            meta = Metadata(
                title = result.get("title", None),
                altTitle = [result.get("localized title", None)],
                cover = result.get("cover url", None),
                summary = result.get("plot outline", None),
                type = 'movie' if result.get('kind', '').lower() == 'movie' else 'serie',
                genres = result.get('genres', [])
            )
            
            return DataResult("SUCCESS", None, meta)
        except Exception as e:
            return DataResult(statusType="ERROR", errorMessage=str(e))