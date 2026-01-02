# search_runner.py
import asyncio
from typing import List
from models.metadata import Metadata
from utils.logger import logger
from sources.factory import get_all_sources

async def run_search(titles: List[str]) -> List[Metadata]:
    """
    Kjører alle kilder parallelt på gitt titler.
    Returnerer en liste av Metadata fra alle kilder.
    Ingen mapping eller scoring gjøres her.
    """

    sources = get_all_sources(titles)

    # Kjør alle kildesøk parallelt
    results = await asyncio.gather(*(s.search() for s in sources), return_exceptions=True)

    metadata_results: List[Metadata] = []
    for source, r in zip(sources, results):
        if isinstance(r, Exception):
            logger.warning(
                f"Kilde '{source.__class__.__name__}' feilet under søk "
                f"med titler={source.titles}: {r}"
            )
        elif r is not None:
            metadata_results.append(r)

    logger.info(f"Søk ferdig: {len(metadata_results)} resultater fra {len(sources)} kilder")
    return metadata_results
