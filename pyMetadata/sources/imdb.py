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
            meta = Metadata()
            meta.title = result.get("title", None)
            meta.altTitle = result.get("localized title", None)
            meta.cover = result.get("cover url", None)
            meta.summary = result.get("plot outline", None)

            airing_format = result.get('kind', '').lower()
            if airing_format == 'movie':
                meta.type = 'movie'
            else:
                meta.type = 'serie'

            meta.genres = result.get('genres', [])
            
            return DataResult("SUCCESS", None, meta)
        except Exception as e:
            return DataResult(statusType="ERROR", errorMessage=str(e))