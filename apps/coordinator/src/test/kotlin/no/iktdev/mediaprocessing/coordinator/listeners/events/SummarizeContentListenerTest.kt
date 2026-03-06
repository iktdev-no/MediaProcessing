package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.every
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class SummarizeContentListenerTest : TestBase() {

    private fun listener() = SummarizeContentListener(coordinatorEnv)

    @DisplayName(
        """
        Hvis event ikke er CollectedEvent
        Når onEvent kalles
        Så:
            skal null returneres
        """
    )
    @Test
    fun ignoreNonCollectedEvent() {
        val result = listener().onEvent(
            event = DummyEvent(),
            history = history
        )
        assertNull(result)
    }

    @DisplayName(
    """
        Hvis parsed mangler i historikken
        Når onEvent kalles
        Så:
            skal det kastes IllegalArgumentException
    """
    )
    @Test
    fun missingParsed_throws() {
        val started = defaultStartEvent().newReferenceId().addToHistory()

        val collected = CollectedEvent(
            eventIds = history.map { it.eventId }.toSet(),
        ).derivedOf(started)

        assertThrows(NoSuchElementException::class.java) {
            listener().onEvent(collected, history)
        }
    }

    @DisplayName(
        """
    Hvis parsed finnes men metadata mangler
    Når onEvent kalles
    Så:
        skal summary fortsatt produseres
    """
    )
    @Test
    fun missingMetadata_stillProducesSummary() {
        val started = defaultStartEvent().newReferenceId().addToHistory()

        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "MyShow",
                parsedFileName = "episode1",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Serie
            )
        ).derivedOf(started).addToHistory()

        val encode = ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = "/tmp/cache/video.mp4"
            ),
            status = TaskStatus.Completed
        ).derivedOf(parsed).addToHistory()

        val collected = CollectedEvent(
            eventIds = history.map { it.eventId }.toSet(),
        ).derivedOf(parsed)

        val result = listener().onEvent(collected, history)

        assertNotNull(result)
        assertTrue(result is ContinuationSummaryEvent)
    }



    @DisplayName(
        """
        Hvis alle nødvendige events finnes
        Når onEvent kalles
        Så:
            skal ContinuationSummaryEvent returneres
        """
    )
    @Test
    fun summaryEvent_isReturned() {
        val outbox = File("./tmp/outbox")
        outbox.mkdirs()
        every { coordinatorEnv.outboxFolder } returns outbox

        val defaultStart = defaultStartEvent().newReferenceId()
            .addToHistory()

        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "MyShow",
                parsedFileName = "episode1",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Serie
            )
        ).derivedOf(defaultStart).addToHistory()

        val metadata = MetadataSearchResultEvent(
            results = emptyList(),
            recommended = MetadataSearchResultEvent.SearchResult(
                searchTitles = listOf("MyShow"),
                similarity = 100,
                prefix = 0,
                keywordScore = 0.0,
                typeScore = 0.0,
                completenessScore = 0.0,
                sourceScore = 0.0,
                totalScore = 0.0,
                metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                    source = "test",
                    title = "MyShow",
                    alternateTitles = emptyList(),
                    cover = null,
                    bannerImage = null,
                    type = MediaType.Serie,
                    summary = emptyList(),
                    genres = emptyList()
                )
            ),
            status = TaskStatus.Completed
        ).derivedOf(parsed).addToHistory()

        val encode = ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = "/tmp/cache/video.mp4"
            ),
            status = TaskStatus.Completed
        ).derivedOf(metadata).addToHistory()

        val collected = CollectedEvent(
            eventIds = history.map { it.eventId }.toSet()
        ).derivedOf(encode).addToHistory()

        val result = listener().onEvent(collected, history)
        assertNotNull(result)
        assertTrue(result is ContinuationSummaryEvent)

        val summary = result as ContinuationSummaryEvent
        assertEquals("MyShow", summary.data.collection)
        assertEquals("MyShow", summary.plan.collection)
        assertNotNull(summary.plan.videoContent)
    }

    @DisplayName(
        """
        Hvis video-, undertekst- og coverfiler finnes
        Når onEvent kalles
        Så:
            skal migreringsplanen inneholde alle filene
        """
    )
    @Test
    fun migrationPlan_containsAllFiles() {
        val outbox = File("./tmp/outbox")
        outbox.mkdirs()
        every { coordinatorEnv.outboxFolder } returns outbox
        val started = defaultStartEvent().newReferenceId()
            .addToHistory()

        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "MyShow",
                parsedFileName = "episode1",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Serie
            )
        ).derivedOf(started).addToHistory()

        val metadata = MetadataSearchResultEvent(
            results = emptyList(),
            recommended = MetadataSearchResultEvent.SearchResult(
                searchTitles = listOf("MyShow"),
                similarity = 100,
                prefix = 0,
                keywordScore = 0.0,
                typeScore = 0.0,
                completenessScore = 0.0,
                sourceScore = 0.0,
                totalScore = 0.0,
                metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                    source = "test",
                    title = "MyShow",
                    alternateTitles = emptyList(),
                    cover = null,
                    bannerImage = null,
                    type = MediaType.Serie,
                    summary = emptyList(),
                    genres = emptyList()
                )
            ),
            status = TaskStatus.Completed
        ).derivedOf(parsed).addToHistory()

        val encode = ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = "/tmp/cache/video.mp4"
            ),
            status = TaskStatus.Completed
        ).derivedOf(metadata).addToHistory()

        val extract = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                language = "eng",
                cachedOutputFile = "/tmp/cache/sub.srt"
            )
        ).derivedOf(encode).addToHistory()

        val cover = CoverDownloadResultEvent(
            data = CoverDownloadResultEvent.CoverDownloadedData(
                source = "test",
                outputFile = "/tmp/cache/cover.jpg"
            ),
            status = TaskStatus.Completed
        ).derivedOf(extract).addToHistory()

        val collected = CollectedEvent(
            eventIds = history.map { it.eventId }.toSet()
        ).derivedOf(cover)

        val result = listener().onEvent(collected, history)
        assertNotNull(result)

        val summary = result as ContinuationSummaryEvent

        assertNotNull(summary.plan.videoContent)
        assertEquals(1, summary.plan.subtitleContent?.size)
        assertEquals(1, summary.plan.coverContent?.size)
    }
}
