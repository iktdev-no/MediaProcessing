import logging
from typing import Dict, List, Optional

import asyncio
from mal import Anime, AnimeSearch, AnimeSearchResult  # type: ignore

from models.metadata import Metadata, Summary
from .source import SourceBase

log = logging.getLogger(__name__)


class Mal(SourceBase):


    def __init__(self, titles: List[str]) -> None:
        super().__init__(titles)

    @property
    def name(self) -> str:
        return "mal"

    async def queryIds(self, title: str) -> Dict[str, str]:
        """
        Returnerer {mal_id: tittel} for en gitt søketittel.
        Ingen fuzzy-scoring her – scoreren tar seg av relevans senere.
        """
        id_to_title: Dict[str, str] = {}

        try:
            search: AnimeSearch = await asyncio.to_thread(AnimeSearch, title) # type: ignore

            capped_results: List[AnimeSearchResult] = search.results[:5] # type: ignore

            for item in capped_results: # type: ignore
                if item.mal_id not in id_to_title: # type: ignore
                    log.info(f"MAL -> id {item.mal_id} = '{item.title}' for søk '{title}'")  # type: ignore
                    id_to_title[str(item.mal_id)] = item.title # type: ignore

        except Exception as e:
            log.exception(f"{self.name} search failed for '{title}': {e}")

        if not id_to_title:
            log.warning(f"{self.name} returned no IDs for '{title}'")

        return id_to_title

    async def fetchMetadata(self, id: str) -> Optional[Metadata]:
        try:
            anime: Anime = await asyncio.to_thread(Anime, id) # type: ignore

            media_type = self.validateMediaTypeOrDrop(anime.type, id, anime.title) # type: ignore
            if media_type is None:
                log.warning(f"{self.name} dropped id {id} ('{anime.title}') due to unsupported media type '{anime.type}'") # type: ignore
                return None

            return Metadata(
                sourceId=str(id),
                title=anime.title, # type: ignore
                altTitle=[
                    alt_name
                    for alt_name in [anime.title_english, *anime.title_synonyms] # type: ignore
                    if alt_name
                ],
                cover=anime.image_url, # type: ignore
                bannerImage=None,
                summary=[
                    Summary(language="eng", summary=anime.synopsis) # type: ignore
                ] if anime.synopsis else [], # type: ignore
                type=media_type,
                genres=anime.genres, # type: ignore
                source="mal",
            )

        except Exception as e:
            log.exception(f"{self.name} metadata fetch failed for id {id}: {e}")
            return None
