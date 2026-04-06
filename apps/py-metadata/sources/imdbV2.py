import logging
import asyncio
from typing import Dict, List, Optional, Any

from imdbinfo import search_title, get_movie, get_akas  # type: ignore
from imdbinfo.locale import set_locale  # type: ignore

from models.metadata import Metadata, Summary
from .source import SourceBase

log = logging.getLogger(__name__)


class ImdbV2(SourceBase):
    """
    Modern IMDb source using imdbinfo (2024+ compatible).
    Supports locale, AKAs, and robust metadata extraction.
    """

    def __init__(self, titles: List[str], locale: Optional[str] = None) -> None:
        super().__init__(titles)
        self.locale = locale

        if locale:
            try:
                set_locale(locale)
                log.info(f"[imdbv2] Locale set to '{locale}'")
            except Exception as e:
                log.warning(f"[imdbv2] Failed to set locale '{locale}': {e}")

    @property
    def name(self) -> str:
        return "imdbv2"

    async def queryIds(self, title: str) -> Dict[str, str]:
        id_to_title: Dict[str, str] = {}

        try:
            results = await asyncio.to_thread(
                search_title,
                title,
                self.locale if self.locale else None
            )

            titles = getattr(results, "titles", [])[:7]

            for item in titles:
                imdb_id = item.imdb_id
                imdb_title = item.title

                if imdb_id and imdb_title:
                    id_to_title[imdb_id] = imdb_title
                    log.info(f"[imdbv2] -> id {imdb_id} = '{imdb_title}' for søk '{title}'")

        except Exception as e:
            log.exception(f"[imdbv2] Search failed for '{title}': {e}")

        if not id_to_title:
            log.warning(f"[imdbv2] No IMDb IDs found for '{title}'")

        return id_to_title

    async def fetchMetadata(self, id: str) -> Optional[Metadata]:
        try:
            movie = await asyncio.to_thread(
                get_movie,
                id,
                self.locale if self.locale else None
            )

            if not movie:
                log.warning(f"[imdbv2] No movie object returned for id {id}")
                return None

            # Validate media type
            media_type = self.validateMediaTypeOrDrop(
                movie.kind,
                id,
                movie.title
            )
            if media_type is None:
                log.warning(f"[imdbv2] Dropped id {id} due to unsupported kind '{movie.kind}'")
                return None

            # Cover
            cover = getattr(movie, "poster_url", None) or getattr(movie, "image", None)

            # Summary
            summary_text = getattr(movie, "plot", None)

            # AKAs (correct handling)
            try:
                akas_data = await asyncio.to_thread(get_akas, id)
                akas_list = [aka.title for aka in akas_data.akas]
            except Exception as e:
                log.warning(f"[imdbv2] Failed to fetch AKAs for id {id}: {e}")
                akas_list = []

            return Metadata(
                sourceId=str(id),
                title=movie.title,
                altTitle=akas_list,
                cover=cover or "",
                bannerImage=None,
                summary=[Summary(language="eng", summary=summary_text)] if summary_text else [],
                type=media_type,
                genres=getattr(movie, "genres", []),
                source="imdbv2",
            )

        except Exception as e:
            log.exception(f"[imdbv2] Metadata fetch failed for id {id}: {e}")
            return None
