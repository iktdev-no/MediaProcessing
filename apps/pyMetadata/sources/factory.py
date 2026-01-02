from typing import List
from .mal import Mal
from .anii import Anii
from .imdb import Imdb
from .source import SourceBase

def get_all_sources(titles: List[str]) -> List[SourceBase]:
    """
    Returnerer alle aktive kilder som implementerer SourceBase.
    """
    return [
        Mal(titles),
        Anii(titles),
        Imdb(titles),
    ]
