import uuid
from datetime import datetime

from tabulate import tabulate
from models.metadata import Metadata
from models.task import MetadataSearchTask
from utils.logger import logger
from models.event import (
    EventMetadata,
    MetadataSearchResultEvent,
    SearchResult,
    MetadataResult,
    Summary,
    TaskStatus,
    MediaType,
)
from worker.search_runner import run_search
from algo.SimpleMatcher import SimpleMatcher
from algo.PrefixMatcher import PrefixMatcher
from algo.AdvancedMatcher import AdvancedMatcher
from db.repository import mark_failed

def source_priority(source: str) -> int:
    """Domene-spesifikk kildevekting."""
    priority_map = {'mal': 1, 'anii': 2, 'imdb': 3}
    return priority_map.get(source, 4)


def score_metadata_against_title(title, m: Metadata) -> SearchResult:
    simple = SimpleMatcher(title, m).getScore()
    prefix = PrefixMatcher(title, m).getScore()
    advanced = AdvancedMatcher(title, m).getScore()

    # IMPORTANT: map exactly to bannerImage, not banner.
    metadata_result = MetadataResult(
        source=m.source,
        title=m.title,
        alternateTitles=m.altTitle if m.altTitle else [],
        cover=getattr(m, "cover", None),
        bannerImage=getattr(m, "bannerImage", None),  # no renaming
        type=m.type,  # must already be MediaType
        summary=[Summary(language=s.language, description=s.summary) for s in m.summary],
        genres=m.genres,
    )

    return SearchResult(
        simpleScore=simple,
        prefixScore=prefix,
        advancedScore=advanced,
        sourceWeight=1.0,
        metadata=metadata_result
    )


def print_summary(results: list[SearchResult], titles: list[str]) -> None:
    """Print tabell med scorer for alle kombinasjoner."""
    rows = []
    for r in results:
        rows.append((
            # NB: metadata.title er matched title, search_title kan du lagre i SearchResult hvis du vil
            r.metadata.title,
            r.metadata.source,
            r.simpleScore,
            r.prefixScore,
            r.advancedScore
        ))
    headers = ["Matched Title", "Source", "Simple", "Prefix", "Advanced"]
    print(tabulate(rows, headers=headers))


def choose_recommended(results: list[SearchResult]) -> SearchResult:
    """Velg recommended basert på scorer og kildevekting."""
    return max(
        results,
        key=lambda r: (
            r.advancedScore,
            r.simpleScore,
            r.prefixScore,
            -source_priority(r.metadata.source)
        )
    )


async def process_task(db, task: MetadataSearchTask) -> MetadataSearchResultEvent|None:
    titles = task.data.searchTitles
    logger.info(f"Prosesserer task {task.taskId} med titler: {titles}")

    try:
        metadata_list = await run_search(titles)
        if not metadata_list:
            mark_failed(db, task.taskId)
            return

        # 1) Score alle kombinasjoner
        results = []
        for m in metadata_list:
            for t in titles:
                results.append(score_metadata_against_title(t, m))

        # 2) Print tabell
        print_summary(results, titles)

        # 3) Velg recommended
        recommended = choose_recommended(results)

        # 4) Bygg event
        core_metadata = EventMetadata(
            created=datetime.now(),
            derivedFromId={task.referenceId, task.taskId}
        )

        event = MetadataSearchResultEvent(
            referenceId=task.referenceId,
            eventId=uuid.uuid4(),
            metadata=core_metadata,
            results=results,
            recommended=recommended,
            status=TaskStatus.COMPLETED
        )

        # 5) Returner
        logger.info(f"✅ Task {task.taskId} ferdig prosessert med {len(results)} resultater")
        return event

    except Exception as e:
        logger.error(f"❌ Task {task.taskId} feilet: {e}")
        mark_failed(db, task.taskId)
        return None