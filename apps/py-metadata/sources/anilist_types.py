from typing import TypedDict, Optional, List


class AniListTitle(TypedDict, total=False):
    english: Optional[str]
    romaji: Optional[str]
    native: Optional[str]


class AniListCoverImage(TypedDict, total=False):
    extraLarge: Optional[str]
    large: Optional[str]
    medium: Optional[str]


class AniListMedia(TypedDict, total=False):
    id: int
    title: AniListTitle
    coverImage: AniListCoverImage
    bannerImage: Optional[str]
    description: Optional[str]
    format: Optional[str]
    genres: List[str]


class AniListPage(TypedDict, total=False):
    media: List[AniListMedia]


class AniListSearchResponse(TypedDict, total=False):
    Page: AniListPage


class AniListResponse(TypedDict, total=False):
    data: Optional[AniListSearchResponse]
