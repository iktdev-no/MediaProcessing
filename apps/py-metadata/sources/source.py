import logging
from abc import ABC, abstractmethod
from typing import List, Dict, Optional

from models.enums import MediaType
from models.metadata import Metadata

log = logging.getLogger(__name__)

class SourceBase(ABC):
    def __init__(self, titles: List[str]) -> None:
        self.titles = titles
        self.found_ids: Dict[str, str] = {}   # NYTT

    @property 
    @abstractmethod 
    def name(self) -> str: 
        """Returner navnet på sourcen, f.eks. 'anii'.""" 
        pass

    @abstractmethod
    async def queryIds(self, title: str) -> Dict[str, str]:
        pass

    @abstractmethod
    async def fetchMetadata(self, id: str) -> Optional[Metadata]:
        pass

    async def search(self) -> List[Metadata]:
        id_to_title: Dict[str, str] = {}

        # 1. Query IDs
        for title in self.titles:
            try:
                ids = await self.queryIds(title)
                for id, found_title in ids.items():
                    id_to_title[id] = found_title
            except Exception as e:
                log.warning(f"{self.__class__.__name__} failed on '{title}': {e}")

        self.found_ids = id_to_title  # ← LAGRE ID-ENE

        if not id_to_title:
            self.logNoMatch(self.__class__.__name__, self.titles)
            return []

        # 2. Fetch metadata
        results: List[Metadata] = []
        for id in id_to_title.keys():
            try:
                meta = await self.fetchMetadata(id)
                if meta:
                    results.append(meta)
            except Exception as e:
                log.warning(f"Failed to fetch metadata for ID {id}: {e}")

        return results


    def logNoMatch(self, source: str, titles: List[str]) -> None:
        combined = ", ".join(titles)
        log.info(f"No match in source {source} for titles: {combined}")

    def parseMediaType(self, raw: str | None) -> Optional[MediaType]:
        """
        Robust type-mapper for alle sourcer.
        Logger outliers og returnerer None hvis type ikke kan utledes.
        """
        t = (raw or "").strip().lower()

        if not t:
            log.warning(f"[{self.name}] Mangler media-type (None eller tom streng)")
            return None

        if "movie" in t:
            return MediaType.MOVIE

        known_series = {x.lower() for x in {
            "tv", "tv series", "tv mini series", "ona", "ova",
            "special", "music", "series", "short", "video",
            "episode", "video game", "tvSeries"
        }}

        if t in known_series:
            return MediaType.SERIE

        log.warning(f"[{self.name}] Uventet media-type '{raw}' – kan ikke utledes type")
        return None


    def validateMediaTypeOrDrop(self, raw_type: str | None, id: str, title: str | None) -> Optional[MediaType]:
        media_type = self.parseMediaType(raw_type)
        if media_type is None:
            log.warning(
                f"[{self.name}] Dropper metadata for id={id} "
                f"('{title}') fordi media-type '{raw_type}' ikke kan utledes"
            )
        return media_type
