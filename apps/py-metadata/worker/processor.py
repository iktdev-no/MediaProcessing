import uuid
from utils.time import utc_now

from algo.MetadataScorer import MetadataScorer
from tabulate import tabulate
from models.task import MetadataSearchTask
from utils.logger import logger
from models.event import (
    EventMetadata,
    MetadataSearchResultEvent,
    SearchResult,
    TaskStatus,
)
from worker.search_runner import run_search
from db.repository import mark_failed


def print_summary(results: list[SearchResult], titles: list[str]) -> None:
    """Print tabell med scorer for alle metadata-resultater."""
    rows = []
    for r in results:
        rows.append((
            ", ".join(r.searchTitles),   # liste → lesbar streng
            r.metadata.title,
            r.metadata.source,
            r.similarity,
            r.prefix,
            r.keywordScore,
            r.typeScore,
            r.completenessScore,
            r.sourceScore,
            r.totalScore,
        ))

    headers = [
        "Search Titles",
        "Matched Title",
        "Source",
        "Similarity",
        "Prefix",
        "Keyword",
        "Type",
        "Complete",
        "SourcePrio",
        "Total"
    ]

    print(tabulate(rows, headers=headers))


def choose_recommended(results: list[SearchResult]) -> SearchResult | None:
    valid_results = [r for r in results if r.totalScore >= 0]
    return max(valid_results, key=lambda r: r.totalScore) if valid_results else None


async def process_task(db, task: MetadataSearchTask) -> MetadataSearchResultEvent | None:
    titles = task.data.searchTitles
    logger.info(f"Prosesserer task {task.taskId} med titler: {titles}")

    try:
        # 1) Hent metadata (cached + fresh)
        metadata_list = await run_search(db, titles)
        if not metadata_list:
            mark_failed(db, task.taskId)
            return

        # 2) Score alle metadata mot ALLE søketitler
        scorer = MetadataScorer()
        results: list[SearchResult] = []
        expected_type = task.data.mediaType

        for m in metadata_list:
            sr = scorer.score(titles, m, expected_type)
            results.append(sr)


        # 3) Print tabell
        print_summary(results, titles)

        # 4) Velg recommended
        recommended = choose_recommended(results)

        # 5) Bygg event
        core_metadata = EventMetadata(
            created=utc_now(),
            derivedFromId={task.taskId, *task.metadata.derivedFromId}
        )


        event = MetadataSearchResultEvent(
            referenceId=task.referenceId,
            eventId=uuid.uuid4(),
            metadata=core_metadata,
            results=results,
            recommended=recommended,
            status=TaskStatus.COMPLETED
        )

        logger.info(
            f"✅ Task {task.taskId} ferdig prosessert med {len(results)} resultater"
        )
        return event

    except Exception as e:
        logger.exception(f"❌ Task {task.taskId} feilet: {e}")
        mark_failed(db, task.taskId)
        return None
