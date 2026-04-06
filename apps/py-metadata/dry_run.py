import asyncio
import uuid
from utils.time import utc_now

from algo.MetadataScorer import MetadataScorer
from tabulate import tabulate
from models.task import MetadataSearchTask, MetadataSearchData
from utils.logger import logger
from models.event import (
    EventMetadata,
    MetadataSearchResultEvent,
    SearchResult,
    TaskStatus,
)
from worker.search_runner import run_search
from db.repository import mark_failed
from tests.fakes.fake_db import FakeDB
from sources.registry import init_source_config


def print_summary(results: list[SearchResult], titles: list[str]) -> None:
    rows = []
    for r in results:
        rows.append((
            ", ".join(r.searchTitles),
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

    print()
    print("=== SCORE BREAKDOWN ===")
    print(tabulate(rows, headers=headers))
    print()


def choose_recommended(results: list[SearchResult]) -> SearchResult:
    return max(results, key=lambda r: r.totalScore)


async def process_task(db, task: MetadataSearchTask) -> MetadataSearchResultEvent | None:
    titles = task.data.searchTitles
    logger.info(f"Prosesserer task {task.taskId} med titler: {titles}")

    try:
        metadata_list = await run_search(db, titles)
        if not metadata_list:
            mark_failed(db, task.taskId)
            return None

        scorer = MetadataScorer()
        results: list[SearchResult] = []
        expected_type = task.data.mediaType

        for m in metadata_list:
            sr = scorer.score(titles, m, expected_type)
            results.append(sr)

        print_summary(results, titles)

        recommended = choose_recommended(results)

        core_metadata = EventMetadata(
            created=utc_now(),
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

        logger.info(f"✅ Task {task.taskId} ferdig prosessert med {len(results)} resultater")
        return event

    except Exception as e:
        logger.error(f"❌ Task {task.taskId} feilet: {e}")
        mark_failed(db, task.taskId)
        return None


async def dry_run():
    print("=== DRY RUN (prod‑style) ===")

    # 1) Lag en fake task slik prod gjør
    task = MetadataSearchTask(
        referenceId=uuid.uuid4(),
        taskId=uuid.uuid4(),
        task="metadata.search",
        status=TaskStatus.PENDING,
        claimed=False,
        claimedBy=None,
        consumed=False,
        lastCheckIn=None,
        persistedAt=utc_now(),
        data=MetadataSearchData(
            searchTitles=[
                "The Wrecking Crew",
            ],
            collection="Crew",
            mediaType="Movie"
        )
    )

    db = FakeDB()

    # 2) Kjør hele prosessen
    event = await process_task(db, task)

    if not event:
        print("❌ Ingen event generert")
        return

    # 3) Print recommended
    print("=== RECOMMENDED ===")
    print(f"{event.recommended.metadata.source} → {event.recommended.metadata.title}")
    print(f"Total score: {event.recommended.totalScore}")
    print()

    # 4) Print hele eventet
    print("=== EVENT PAYLOAD ===")
    print(event.model_dump_json(indent=2))


if __name__ == "__main__":
    logger.info("🔧 Laster source-config...")
    init_source_config()
    asyncio.run(dry_run())
