# search_runner.py

import asyncio
from typing import List
from models.metadata import Metadata
from utils.logger import logger
from sources.factory import get_all_sources
from db.metadata_repository import fetch_metadata_by_source_and_id


async def run_search(db, titles: List[str]) -> List[Metadata]:
    sources = get_all_sources(titles)
    metadata_results: List[Metadata] = []
    # 1. First: query IDs for all sources
    for source in sources:
        for title in titles:
            try:
                ids = await source.queryIds(title)
                source.found_ids.update(ids)
            except Exception as e:
                logger.warning(f"{source.name} failed on {title}: {e}")

    # 2. Check cache for each ID
    fetch_tasks = []

    for source in sources:
        for source_id in source.found_ids.keys():
            cached = fetch_metadata_by_source_and_id(db, source.name, source_id)

            if cached:
                logger.info(f"Cache hit for {source.name}:{source_id}")
                metadata_results.append(cached)
            else:
                # fetch only missing metadata
                fetch_tasks.append(source.fetchMetadata(source_id))

    # 3. Fetch missing metadata
    fresh_results = await asyncio.gather(*fetch_tasks, return_exceptions=True)

    for result in fresh_results:
        if isinstance(result, Exception):
            logger.warning(f"Source failed: {result}")
            continue
        if result:
            metadata_results.append(result)

    logger.info(
        f"Søk ferdig: {len(metadata_results)} metadata-objekter "
        f"(cached + fresh)"
    )

    return metadata_results
