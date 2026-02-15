import logging
import hashlib
import asyncio
from typing import List, Dict, Optional

from AnilistPython import Anilist

from models.enums import MediaType
from models.metadata import Metadata, Summary
from .source import SourceBase

log = logging.getLogger(__name__)


class Anii(SourceBase):
    """
    AniListPython har ikke et ekte søk som returnerer flere kandidater.
    Derfor:
      - queryIds() returnerer maks 1 ID per tittel
      - fetchMetadata() henter metadata basert på cached result
    """

    def __init__(self, titles: List[str]) -> None:
        super().__init__(titles)
        self.api = Anilist()
        self._cache: Dict[str, Dict] = {}  # id -> raw result


    @property
    def name(self) -> str:
        return "anii"


    async def queryIds(self, title: str) -> Dict[str, str]:
        """
        AniListPython.get_anime(title) returnerer kun ett resultat.
        Vi genererer en stabil ID basert på tittelen.
        """
        id_to_title: Dict[str, str] = {}

        try:
            result = await asyncio.to_thread(self.api.get_anime, title)

            if not result:
                return {}

            # Finn engelsk eller romaji tittel
            use_title = result.get("name_english") or result.get("name_romaji")
            if not use_title:
                return {}

            # Generer en stabil ID basert på tittelen
            generated_id = self.generate_id(use_title)
            if not generated_id:
                return {}

            # Cache raw result slik at fetchMetadata kan hente det
            self._cache[generated_id] = result

            id_to_title[generated_id] = use_title

            log.info(f"AniList -> id {generated_id} = '{use_title}' for søk '{title}'")

        except Exception as e:
            if "429" in str(e):
                log.error("AniList rate limited")
            else:
                log.exception(e)

        return id_to_title

    async def fetchMetadata(self, id: str) -> Optional[Metadata]:
        """
        Henter metadata fra cache (fordi AniListPython ikke har get_by_id).
        """
        result = self._cache.get(id)
        if not result:
            return None

        try:
            use_title = result.get("name_english") or result.get("name_romaji")

            # Felles media-type validering
            media_type = self.validateMediaTypeOrDrop(result.get("airing_format"), id, use_title)
            if media_type is None:
                return None

            summary = result.get("desc")
            if not use_title:
                return None

            alt_titles = []
            if result.get("name_romaji") and result.get("name_romaji") != use_title:
                alt_titles.append(result.get("name_romaji"))

            return Metadata(
                sourceId=str(id),
                title=use_title,
                altTitle=alt_titles,
                cover=result.get("cover_image"),
                bannerImage=None,
                summary=[Summary(language="eng", summary=summary)] if summary else [],
                type=media_type,
                genres=result.get("genres", []),
                source="anii",
            )

        except Exception as e:
            log.exception(e)
            return None


    def generate_id(self, text: str) -> Optional[str]:
        if text:
            return hashlib.md5(text.encode()).hexdigest()
        return None
