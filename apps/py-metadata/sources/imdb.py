import logging
import asyncio
from typing import List, Dict, Optional

from imdb import Cinemagoer
from imdb.Movie import Movie

from models.enums import MediaType
from models.metadata import Metadata, Summary
from .source import SourceBase

log = logging.getLogger(__name__)


class Imdb(SourceBase):
    def __init__(self, titles: List[str]) -> None:
        super().__init__(titles)
        self.api = Cinemagoer(accessSystem="http")

    @property
    def name(self) -> str:
        return "imdb"


    async def queryIds(self, title: str) -> Dict[str, str]:
        """
        Returnerer {imdb_id: tittel} for en gitt søketittel.
        Ingen fuzzy scoring – scoreren tar seg av relevans senere.
        """
        id_to_title: Dict[str, str] = {}

        try:
            search_results = await asyncio.to_thread(self.api.search_movie, title)
            capped: List[Movie] = search_results[:7]  # IMDb kan være støyete

            for item in capped:
                imdb_id = item.movieID
                imdb_title = item.get("title")

                if imdb_id not in id_to_title:
                    log.info(f"IMDB -> id {imdb_id} = '{imdb_title}' for søk '{title}'")
                    id_to_title[imdb_id] = imdb_title

        except Exception as e:
            log.exception(e)

        return id_to_title


    async def fetchMetadata(self, id: str) -> Optional[Metadata]:
        """
        Henter full metadata for en gitt IMDB-id.
        Kalles av SourceBase.search() for ALLE kandidater.
        """
        try:
            result = await asyncio.to_thread(self.api.get_movie, id)

            # Felles media-type validering
            media_type = self.validateMediaTypeOrDrop(result.get("kind"), id, result.get("title"))
            if media_type is None:
                return None

            # Cover fallback
            cover = result.get_fullsizeURL() or result.get("cover url")

            # Synopsis
            summary = result.get("plot outline")

            # Alternative titler
            localized_titles = result.get("localized title")
            alt_titles = localized_titles if isinstance(localized_titles, list) else []

            return Metadata(
                sourceId=str(id),
                title=result.get("title"),
                altTitle=alt_titles,
                cover=cover,
                bannerImage=None,
                summary=[Summary(language="eng", summary=summary)] if summary else [],
                type=media_type,
                genres=result.get("genres", []),
                source="imdb",
            )

        except Exception as e:
            log.exception(e)
            return None
