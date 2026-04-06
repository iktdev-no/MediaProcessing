import logging
import asyncio
import requests
from typing import Dict, List, Optional

from models.metadata import Metadata, Summary
from .source import SourceBase

from .anilist_types import (
    AniListResponse,
    AniListMedia,
)

log = logging.getLogger(__name__)

ANILIST_URL = "https://graphql.anilist.co"


class AniiV2(SourceBase):
    """
    AniList GraphQL-basert source.
    Typesikker, eksplisitt og robust.
    """

    @property
    def name(self) -> str:
        return "aniiv2"

    async def queryIds(self, title: str) -> Dict[str, str]:
        query = """
        query ($search: String) {
          Page(perPage: 10) {
            media(search: $search, type: ANIME) {
              id
              title {
                english
                romaji
                native
              }
            }
          }
        }
        """

        variables = {"search": title}

        try:
            response = await asyncio.to_thread(
                requests.post,
                ANILIST_URL,
                json={"query": query, "variables": variables},
                timeout=10
            )

            data: AniListResponse = response.json()  # type: ignore[assignment]

            data_block = data.get("data")
            if not data_block:
                log.warning(f"AniListV2 returned no data block for '{title}'")
                return {}

            page_block = data_block.get("Page")
            if not page_block:
                log.warning(f"AniListV2 returned no Page block for '{title}'")
                return {}

            media_list: List[AniListMedia] = page_block.get("media", [])


            id_to_title: Dict[str, str] = {}

            for item in media_list:
                anime_id = item.get("id")
                title_block = item.get("title")

                if anime_id is None or not title_block:
                    log.warning(f"AniListV2: Skipping malformed media entry: {item}")
                    continue

                chosen_title = (
                    title_block.get("english")
                    or title_block.get("romaji")
                    or title_block.get("native")
                )

                if chosen_title:
                    id_to_title[str(anime_id)] = chosen_title
                    log.info(f"AniListV2 -> id {anime_id} = '{chosen_title}' for søk '{title}'")


            if not id_to_title:
                log.warning(f"AniListV2 returned no IDs for '{title}'")

            return id_to_title

        except Exception as e:
            log.exception(f"AniListV2 search failed for '{title}': {e}")
            return {}

    async def fetchMetadata(self, id: str) -> Optional[Metadata]:
        query = """
        query ($id: Int) {
        Media(id: $id, type: ANIME) {
            id
            title {
            english
            romaji
            native
            }
            coverImage {
            extraLarge
            large
            medium
            }
            bannerImage
            description
            format
            genres
        }
        }
        """

        variables = {"id": int(id)}

        try:
            response = await asyncio.to_thread(
                requests.post,
                ANILIST_URL,
                json={"query": query, "variables": variables},
                timeout=10
            )

            data: AniListResponse = response.json()  # type: ignore[assignment]

            data_block = data.get("data") 
            if not data_block:
                log.warning(f"AniListV2 returned no data block for id {id}")
                return None

            media: Optional[AniListMedia] = data_block.get("Media")
            if not media:
                log.warning(f"AniListV2 returned no Media for id {id}")
                return None

            title_data = media.get("title")
            if not title_data:
                log.warning(f"AniListV2 returned media without title for id {id}")
                return None
            
            title = (
                title_data.get("english")
                or title_data.get("romaji")
                or title_data.get("native")
            )
            if not title:
                log.warning(f"AniListV2 returned media without title for id {id}")
                return None

            media_type = self.validateMediaTypeOrDrop(media.get("format"), id, title)
            if media_type is None:
                log.warning(f"AniListV2 dropped id {id} ('{title}') due to unsupported media type '{media.get('format')}'")
                return None

            alt_titles = [
                t for t in [
                    title_data.get("romaji"),
                    title_data.get("native")
                ]
                if t and t != title
            ]

            cover_image = media.get("coverImage", {})
            cover = (
                cover_image.get("extraLarge")
                or cover_image.get("large")
                or cover_image.get("medium")
            )

            description = media.get("description") or ""

            return Metadata(
                sourceId=str(id),
                title=title,
                altTitle=alt_titles,
                cover=cover,
                bannerImage=media.get("bannerImage"),
                summary=[Summary(language="eng", summary=description)] if description else [],
                type=media_type,
                genres=media.get("genres", []),
                source="aniiv2",
            )

        except Exception as e:
            log.exception(f"AniListV2 metadata fetch failed for id {id}: {e}")
            return None
