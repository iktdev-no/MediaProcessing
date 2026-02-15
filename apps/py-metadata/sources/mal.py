import logging
from typing import Dict, List, Optional

import asyncio
from mal import Anime, AnimeSearch, AnimeSearchResult

from models.enums import MediaType
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
            # MAL API-kall i egen tråd
            search = await asyncio.to_thread(AnimeSearch, title)

            # Ta de første 5 resultatene – MAL kan være støyete
            capped_results: List[AnimeSearchResult] = search.results[:5]

            for item in capped_results:
                if item.mal_id not in id_to_title:
                    log.info(f"MAL -> id {item.mal_id} = '{item.title}' for søk '{title}'")
                    id_to_title[str(item.mal_id)] = item.title

        except Exception as e:
            log.exception(e)

        return id_to_title


    async def fetchMetadata(self, id: str) -> Optional[Metadata]:
        try:
            anime = await asyncio.to_thread(Anime, id)

            # Bruk felles helper i SourceBase
            media_type = self.validateMediaTypeOrDrop(anime.type, id, anime.title)
            if media_type is None:
                return None

            return Metadata(
                sourceId=str(id),
                title=anime.title,
                altTitle=[
                    alt_name
                    for alt_name in [anime.title_english, *anime.title_synonyms]
                    if alt_name
                ],
                cover=anime.image_url,
                bannerImage=None,
                summary=[
                    Summary(language="eng", summary=anime.synopsis)
                ] if anime.synopsis else [],
                type=media_type,
                genres=anime.genres,
                source="mal",
            )

        except Exception as e:
            log.exception(e)
            return None
