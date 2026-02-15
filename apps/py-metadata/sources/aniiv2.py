import logging
import asyncio
import requests
from typing import Dict, List, Optional

from models.enums import MediaType
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

            data: AniListResponse = response.json()

            media_list = (
                data.get("data", {})
                .get("Page", {})
                .get("media", [])
            )

            id_to_title: Dict[str, str] = {}

            for item in media_list:
                anime_id = str(item["id"])
                t = item["title"]

                chosen_title = (
                    t.get("english")
                    or t.get("romaji")
                    or t.get("native")
                )

                if chosen_title:
                    id_to_title[anime_id] = chosen_title
                    log.info(f"AniListV2 -> id {anime_id} = '{chosen_title}' for søk '{title}'")

            return id_to_title

        except Exception as e:
            log.exception(e)
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

            data: AniListResponse = response.json()
            media: Optional[AniListMedia] = data.get("data", {}).get("Media")

            if not media:
                return None

            title_data = media["title"]
            title = (
                title_data.get("english")
                or title_data.get("romaji")
                or title_data.get("native")
            )

            # Felles media-type validering
            media_type = self.validateMediaTypeOrDrop(media.get("format"), id, title)
            if media_type is None:
                return None

            alt_titles = [
                t for t in [
                    title_data.get("romaji"),
                    title_data.get("native")
                ]
                if t and t != title
            ]

            cover = (
                media["coverImage"].get("extraLarge")
                or media["coverImage"].get("large")
                or media["coverImage"].get("medium")
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
            log.exception(e)
            return None
