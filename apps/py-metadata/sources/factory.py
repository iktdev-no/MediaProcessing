# sources/factory.py
from typing import List

from config.source_activation_loader import load_source_config
from .mal import Mal
from .anii import Anii
from .aniiv2 import AniiV2
from .imdb import Imdb
from .source import SourceBase

def get_all_sources(titles: List[str]) -> List[SourceBase]:
    cfg = load_source_config()

    sources: List[SourceBase] = []

    if cfg.get("aniiv2", False):
        sources.append(AniiV2(titles))

    if cfg.get("mal", False):
        sources.append(Mal(titles))

    if cfg.get("imdb", False):
        sources.append(Imdb(titles))

    if cfg.get("anii", False):
        sources.append(Anii(titles))

    return sources
