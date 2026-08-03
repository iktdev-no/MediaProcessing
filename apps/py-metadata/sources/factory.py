# sources/factory.py
from typing import List

from .mal import Mal
from .anii import Anii
from .imdb import Imdb
from .source import SourceBase
from .tmdb import Tmdb
from sources.registry import SOURCE_CONFIG

def get_all_sources(titles: List[str]) -> List[SourceBase]:
    cfg = SOURCE_CONFIG  # <--- bruker cached config

    sources: List[SourceBase] = []

    if cfg.get("anii", False):
        sources.append(Anii(titles))

    if cfg.get("mal", False):
        sources.append(Mal(titles))

    if cfg.get("imdb", False):
        sources.append(Imdb(titles))

    if cfg.get("tmdb", False):
        sources.append(Tmdb(titles))

    return sources
