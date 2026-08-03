import logging
import asyncio
import os
from typing import Dict, List, Optional, Any

import tmdbsimple as tmdb  # type: ignore

from models.metadata import Metadata, Summary
from models.enums import MediaType
from .source import SourceBase

log = logging.getLogger(__name__)


class Tmdb(SourceBase):
    """
    TMDB source - robust, stable, supports movies + TV.
    Search-only, same job as IMDbV2.
    """

    sourceName = "TMDB"


    def __init__(self, titles: List[str], locale: str = "en-US") -> None:
        super().__init__(titles)
        self.locale = locale

        api_key = os.getenv("TMDB_API_KEY")
        if not api_key:
            raise RuntimeError("TMDB_API_KEY is not set in environment")

        tmdb.API_KEY = api_key

    @property
    def name(self) -> str:
        return "tmdb"

    # ---------------------------------------------------------
    # Query IDs
    # ---------------------------------------------------------

    async def queryIds(self, title: str) -> Dict[str, str]:
        id_to_title: Dict[str, str] = {}

        try:
            search = tmdb.Search()

            response = await asyncio.to_thread(
                search.multi,
                query=title,
                language=self.locale
            )

            results = response.get("results", [])[:7]

            for item in results:
                media_type = item.get("media_type")

                if media_type not in ("movie", "tv"):
                    continue

                tmdb_id = str(item.get("id"))
                tmdb_title = (
                    item.get("title")
                    or item.get("name")
                    or ""
                )

                if tmdb_id and tmdb_title:
                    id_to_title[tmdb_id] = tmdb_title
                    log.info(f"[tmdb] -> id {tmdb_id} = '{tmdb_title}' for søk '{title}'")

        except Exception as e:
            log.exception(f"{self.sourceName} Search failed for '{title}': {e}")

        if not id_to_title:
            log.warning(f"{self.sourceName} No TMDB IDs found for '{title}'")

        return id_to_title

    # ---------------------------------------------------------
    # Fetch metadata
    # ---------------------------------------------------------

    async def fetchMetadata(self, id: str) -> Optional[Metadata]:
        try:
            # Try movie first
            movie = tmdb.Movies(id)
            movie_data = await asyncio.to_thread(
                movie.info,
                language=self.locale
            )

            if movie_data.get("title"):
                return self._build_movie_metadata(id, movie_data)

            # Try TV
            tv = tmdb.TV(id)
            tv_data = await asyncio.to_thread(
                tv.info,
                language=self.locale
            )

            if tv_data.get("name"):
                return self._build_tv_metadata(id, tv_data)

            log.warning(f"{self.sourceName} No metadata found for id {id}")
            return None

        except Exception as e:
            log.exception(f"{self.sourceName} Metadata fetch failed for id {id}: {e}")
            return None

    # ---------------------------------------------------------
    # Builders
    # ---------------------------------------------------------

    def _build_movie_metadata(self, id: str, data: Dict[str, Any]) -> Optional[Metadata]:
        title = data.get("title")
        overview = data.get("overview")
        poster = data.get("poster_path")
        backdrop = data.get("backdrop_path")

        media_type = self.validateMediaTypeOrDrop(
            "movie",   # <-- FIXED
            id,
            title
        )
        if media_type is None:
            return None

        alt_titles = []
        for entry in data.get("alternative_titles", {}).get("titles", []):
            t = entry.get("title")
            if t and t != title:
                alt_titles.append(t)

        return Metadata(
            sourceId=str(id),
            title=title,
            altTitle=alt_titles,
            cover=f"https://image.tmdb.org/t/p/original{poster}" if poster else "",
            bannerImage=f"https://image.tmdb.org/t/p/original{backdrop}" if backdrop else None,
            summary=[Summary(language="eng", summary=overview)] if overview else [],
            type=media_type,
            genres=[g["name"] for g in data.get("genres", [])],
            source="tmdb",
        )


    def _build_tv_metadata(self, id: str, data: Dict[str, Any]) -> Optional[Metadata]:
        title = data.get("name")
        overview = data.get("overview")
        poster = data.get("poster_path")
        backdrop = data.get("backdrop_path")

        media_type = self.validateMediaTypeOrDrop(
            "serie",
            id,
            title
        )
        if media_type is None:
            return None

        alt_titles = []
        for entry in data.get("alternative_titles", {}).get("results", []):
            t = entry.get("title")
            if t and t != title:
                alt_titles.append(t)

        return Metadata(
            sourceId=str(id),
            title=title,
            altTitle=alt_titles,
            cover=f"https://image.tmdb.org/t/p/original{poster}" if poster else "",
            bannerImage=f"https://image.tmdb.org/t/p/original{backdrop}" if backdrop else None,
            summary=[Summary(language="eng", summary=overview)] if overview else [],
            type=media_type,
            genres=[g["name"] for g in data.get("genres", [])],
            source="tmdb",
        )
