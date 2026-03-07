package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.slot
import io.mockk.verify
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.MockData.convertEvent
import no.iktdev.mediaprocessing.MockData.coverEvent
import no.iktdev.mediaprocessing.MockData.encodeEvent
import no.iktdev.mediaprocessing.MockData.extractEvent
import no.iktdev.mediaprocessing.MockData.mediaParsedEvent
import no.iktdev.mediaprocessing.MockData.metadataEvent
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ContinuationSummaryEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.PersistContentEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MigrateToContentStoreTask
import no.iktdev.mediaprocessing.shared.common.model.ContentMigrationPlan
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.common.model.MigrateStatus
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class MigrateCreateStoreTaskCreateListenerTest : TestBase() {

    private val listener = MigrateCreateStoreTaskCreateListener(coordinatorEnv)

    @Test
    @DisplayName(
        """
    Hvis historikken inneholder gyldig parsed info, metadata og migreringsdata
    Når onEvent kalles med CollectedEvent
    Så:
        Opprettes MigrateToContentStoreTask og sendes til TaskStore.persist
"""
    )
    fun `creates migrate-to-store task`() {
        val started = defaultStartEvent()
            .addToHistory()

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)
            .addToHistory()

        val metadata = metadataEvent(parsed).also { x -> x.forEach { it.addToHistory() } }

        val encode = encodeEvent("/tmp/video.mp4", metadata.last())
            .also { x -> x.forEach { it.addToHistory() } }


        val extract = extractEvent("en", "/tmp/sub1.srt", encode.last())
            .also { x -> x.forEach { it.addToHistory() } }

        val coverDownload = coverEvent("/tmp/cover.jpg", metadata.last())
            .also { x -> x.forEach { it.addToHistory() } }

        val convert = convertEvent(
            language = "en",
            baseName = "sub1",
            outputFiles = listOf("/tmp/sub1.vtt"),
            derivedFrom = extract.last()
        ).also { x -> x.forEach { it.addToHistory() } }

        val migrate = migrateResultEvent(
            collection = "MyCollection",
            videoUri = "file:///video.mp4",
            coverUri = "file:///cover.jpg",
            subtitleUris = listOf("file:///sub1.srt", "file://sub1.vtt")
        ).derivedOf(convert.last())
            .addToHistory()

        val collected = CollectedEvent(
            setOf(
                started.eventId,
                parsed.eventId,
                *metadata.map { it.eventId }.toTypedArray(),
                *encode.map { it.eventId }.toTypedArray(),
                *extract.map { it.eventId }.toTypedArray(),
                *convert.map { it.eventId }.toTypedArray(),
                *coverDownload.map { it.eventId }.toTypedArray(),
                migrate.eventId
            )
        ).derivedOf(migrate)
            .addToHistory()

        val summaryEvent = SummarizeContentListener(coordinatorEnv)
            .onEvent(collected, history)?.also { it.addToHistory() }!!

        val persistContent = PersistContentEvent()
            .derivedOf(summaryEvent)
            .addToHistory()

        val result = listener.onEvent(persistContent, history)

        assertThat(result).isNotNull()

        verify(exactly = 1) {
            TaskStore.persist(withArg { task ->
                val storeTask = task as MigrateToContentStoreTask

                assertThat(storeTask.data.collection).isEqualTo("MyCollection")
                assertThat(storeTask.data.videoContent).isNotNull()
                assertThat(storeTask.data.subtitleContent).hasSize(2)
                assertThat(storeTask.data.coverContent).isNotNull
            })
        }

    }

    @Test
    @DisplayName(
        """
    Hvis historikken inneholder gyldig parsed info, metadata og migreringsdata
    Når onEvent kalles med CollectedEvent
    Så:
        Opprettes MigrateToContentStoreTask og sendes til TaskStore.persist
"""
    )
    fun success1() {
        val started = defaultStartEvent()
            .addToHistory()

        val parsed = mediaParsedEvent(
            collection = "Baking Bread",
            fileName = "Baking Bread - S01E01 - Flour",
            mediaType = MediaType.Serie
        ).derivedOf(started)
            .addToHistory()

        val metadata = metadataEvent(derivedFrom = parsed, mediaType = MediaType.Serie)
            .addToHistory()

        val encode = encodeEvent("/tmp/video.mp4", metadata.last())
            .addToHistory()

        val extract = extractEvent("en", "/tmp/sub1.srt", encode.last())
            .addToHistory()

        val coverDownload = coverEvent("/tmp/cover.jpg", metadata.last())
            .addToHistory()
        val coverDownload2 = coverEvent("/tmp/cover.jpg", metadata.last(), "potet")
            .addToHistory()

        val convert = convertEvent(
            language = "en",
            baseName = "sub1",
            outputFiles = listOf("/tmp/sub1.vtt"),
            derivedFrom = extract.last()
        ).addToHistory()


        val collected = CollectedEvent(
            setOf(
                started.eventId,
                parsed.eventId,
                *metadata.map { it.eventId }.toTypedArray(),
                *encode.map { it.eventId }.toTypedArray(),
                *extract.map { it.eventId }.toTypedArray(),
                *convert.map { it.eventId }.toTypedArray(),
                *coverDownload.map { it.eventId }.toTypedArray(),
                *coverDownload2.map { it.eventId }.toTypedArray(),
            )
        ).derivedOf(coverDownload.last())
            .addToHistory()

        val summaryEvent = SummarizeContentListener(coordinatorEnv)
            .onEvent(collected, history)?.addToHistory()!!

        val persistContent = PersistContentEvent()
            .derivedOf(summaryEvent)
            .addToHistory()

        val result = listener.onEvent(persistContent, history)

        assertThat(result).isNotNull()

        val slot = slot<MigrateToContentStoreTask>()

        verify(exactly = 1) {
            TaskStore.persist(capture(slot))
        }

        val storeTask = slot.captured

        assertThat(storeTask.data.collection).isEqualTo("Baking Bread")
        assertThat(storeTask.data.videoContent).isNotNull()
        assertThat(storeTask.data.videoContent?.storeUri.let { f -> File(f).name })
            .isEqualTo("Baking Bread - S01E01 - Flour.mp4")

        assertThat(storeTask.data.subtitleContent).hasSize(2)
        assertThat(
            storeTask.data.subtitleContent!!
                .map { File(it.storeUri).nameWithoutExtension }
        ).containsOnly("Baking Bread - S01E01 - Flour")

        assertThat(File(storeTask.data.coverContent!!.storeUri).name)
            .isEqualTo("Baking Bread.jpg")


    }


    @Test
    @DisplayName(
    """
        Hvis start hendelsen kun inneholder converter, og vi har gjennomført konvertering
        Når onEvent kalles med CollectedEvent
        Så:
            Opprettes det migrate task
        """
    )
    fun createMigrateForConvert() {
        val started = defaultStartEvent()
            .addToHistory()

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Subtitle
        ).derivedOf(started)
            .addToHistory()

        val convert = convertEvent(
            language = "en",
            baseName = "sub1",
            outputFiles = listOf("/tmp/sub1.vtt"),
            derivedFrom = started
        ).addToHistory()

        val migrate = migrateResultEvent(
            collection = "MyCollection",
            videoUri = "file:///video.mp4",
            coverUri = null,
            subtitleUris = listOf("file:///sub1.srt", "file://sub1.vtt")
        ).derivedOf(convert.last())
            .addToHistory()

        val collected = CollectedEvent(
            setOf(
                started.eventId,
                parsed.eventId,
                *convert.map { it.eventId }.toTypedArray(),
                migrate.eventId,
            )
        ).derivedOf(migrate)


        val summaryEvent = SummarizeContentListener(coordinatorEnv)
            .onEvent(collected, history)?.addToHistory()!!

        val persistContent = PersistContentEvent()
            .derivedOf(summaryEvent)
            .addToHistory()


        val result = listener.onEvent(persistContent, history)

        assertThat(result).isNotNull()

        // ⭐ FANG ARGUMENTET I SLOT
        val slot = slot<MigrateToContentStoreTask>()

        verify(exactly = 1) {
            TaskStore.persist(capture(slot))
        }

        val storeTask = slot.captured

        // ⭐ VANLIGE ASSERTS
        assertThat(storeTask.data.collection).isEqualTo("MyCollection")
        assertThat(storeTask.data.videoContent).isNull()
        assertThat(storeTask.data.subtitleContent).hasSize(1)
        assertThat(storeTask.data.coverContent).isNull()
    }


    // ---------------------------------------------------------
    // Helpers for generating events
    // ---------------------------------------------------------

    private fun migrateResultEvent(
        collection: String,
        videoUri: String?,
        coverUri: String?,
        subtitleUris: List<String>
    ) = MigrateContentToStoreTaskResultEvent(
        status = TaskStatus.Completed,
        migrateData = MigrateContentToStoreTaskResultEvent.MigrateData(
            collection = collection,
            videoMigrate = MigrateContentToStoreTaskResultEvent.FileMigration(
                storedUri = videoUri,
                status = if (videoUri != null) MigrateStatus.Completed else MigrateStatus.Failed
            ),
            subtitleMigrate = subtitleUris.map {
                MigrateContentToStoreTaskResultEvent.SubtitleMigration(
                    language = "en",
                    storedUri = it,
                    status = MigrateStatus.Completed
                )
            },
            coverMigrate =
                coverUri?.let {
                    MigrateContentToStoreTaskResultEvent.FileMigration(
                        storedUri = it,
                        status = MigrateStatus.Completed
                    )
                }

        )
    )

}
