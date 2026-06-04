package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.slot
import io.mockk.verify
import no.iktdev.eventi.events.SoftDispatchException
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.MockData.convertEvent
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.StoreContentAndMetadataTask
import no.iktdev.mediaprocessing.shared.common.model.ContentExport
import no.iktdev.mediaprocessing.shared.common.model.ContentMigrationPlan
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.common.model.MigrateStatus
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StoreContentAndMetadataListenerTest : TestBase() {

    private val listener = StoreContentAndMetadataListener()

    @Test
    @DisplayName(
        """
        Hvis event ikke er et MigrateContentToStoreTaskResultEvent
        Når onEvent kalles
        Så:
            Returneres null
    """
    )
    fun `ignores non migrate events`() {
        val startedEvent = defaultStartEvent().addToHistory()
        val event = DummyEvent().derivedOf(startedEvent).addToHistory()

        val result = listener.onEvent(event, history)
        assertThat(result).isNull()
    }

    @Test
    @DisplayName(
        """
        Hvis historikken ikke inneholder CollectedEvent
        Når onEvent kalles
        Så:
            Returneres null
    """
    )
    fun `returns null when no collected event exists`() {
        val startedEvent = defaultStartEvent().addToHistory()
        val event = migrateEvent().derivedOf(startedEvent).addToHistory()
        val result = listener.onEvent(event, history)
        assertThat(result).isNull()
    }

    @Test
    @DisplayName(
        """
        Hvis collection eller metadata ikke kan projiseres
        Når onEvent kalles
        Så:
            Returneres null
    """
    )
    fun `returns null when projection lacks collection or metadata`() {
        val startedEvent = defaultStartEvent()
        val event = migrateEvent().derivedOf(startedEvent)
        val collected = CollectedEvent(setOf(startedEvent.eventId, event.eventId))

        // Historikken inneholder kun collected-eventet, ingen metadata eller parsed info
        val history = listOf(startedEvent, collected)

        val result = listener.onEvent(event, history)
        assertThat(result).isNull()
    }

    @Test
    @DisplayName(
        """
        Hvis historikken inneholder gyldig collection og metadata
        Når onEvent kalles
        Så:
            Opprettes StoreContentAndMetadataTask og det returneres et StoreContentAndMetadataTaskCreatedEvent
    """
    )
    fun `creates task and returns created event`() {
        val startedEvent = defaultStartEvent()
            .addToHistory()

        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "MyCollection",
                parsedFileName = "MyCollection",
                parsedSearchTitles = listOf("MyCollection"),
                mediaType = MediaType.Serie,
                episodeInfo = null
            )
        ).derivedOf(startedEvent)
            .addToHistory()


        val processEncodeResultEvent = ProcesserEncodeResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = "cache:///Baking Bread/Baking Bread - S01E01 - Flour.mp4"
            )
        ).derivedOf(parsed)
            .addToHistory()


        val collected = CollectedEvent(
            history.map { it.eventId }.toSet()
        )
            .derivedOf(processEncodeResultEvent)
            .addToHistory()

        val summaryEvent = defaultSummaryEvent().derivedOf(collected).addToHistory()!!

        val persistContent = PersistContentEvent()
            .derivedOf(summaryEvent)
            .addToHistory()

        val migratedEvent = MigrateContentToStoreTaskResultEvent(
            migrateData = MigrateContentToStoreTaskResultEvent.MigrateData(
                collection = "Baking Bread",
                videoMigrate = MigrateContentToStoreTaskResultEvent.FileMigration("store://Baking Bread/Baking Bread - S01E01 - Flour.mp4",
                    MigrateStatus.Completed),
                subtitleMigrate = emptyList(),
                coverMigrate = MigrateContentToStoreTaskResultEvent.FileMigration("store://Baking Bread/Baking Bread.jpg",
                    MigrateStatus.Completed),
            ),
            status =  TaskStatus.Completed
        ).derivedOf(persistContent)

        val result = listener.onEvent(migratedEvent, history)
        assertThat(result).isInstanceOf(StoreContentAndMetadataTaskCreatedEvent::class.java)

        val slot = slot<StoreContentAndMetadataTask>()

        verify(exactly = 1) {
            TaskStore.persist(capture(slot))
        }

        val storeTask = slot.captured
        assertThat(storeTask.data.collection).isEqualTo("Baking Bread")
        assertThat(storeTask.data.metadata!!.mediaType).isEqualTo(MediaType.Serie)
        assertThat(storeTask.data.media?.videoFile).isEqualTo("Baking Bread - S01E01 - Flour.mp4")
        assertThat(storeTask.data.media?.subtitles?.first()?.subtitleFile).isEqualTo("Baking Bread - S01E01 - Flour.ass")
        assertThat(storeTask.data.media?.subtitles?.first()?.language).isEqualTo("eng")

    }


    @Test
    @DisplayName(
        """
        Hvis historikken inneholder gyldig collection og kun subtitle som media
        Når onEvent kalles
        Så:
            Opprettes StoreContentAndMetadataTask og det returneres et StoreContentAndMetadataTaskCreatedEvent
    """
    )
    fun createTaskForWhenOnlyStoringSubtitles() {
        val workFolder = IFile("build").using("subby", "eng")

        val started = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.ConvertSubtitles),
                flow = StartFlow.Manual,
                fileUri = workFolder.using("subby.srt").absolutePath,
            )
        ).newReferenceId()
            .addToHistory()

        val convert = convertEvent(
            language = "eng",
            baseName = "subby",
            outputFiles = listOf(workFolder.using("subby.vtt").absolutePath),
            derivedFrom = started
        ).addToHistory()


        val collected = CollectedEvent(
            history.map { it -> it.eventId }.toSet()
        )
            .derivedOf(convert.last())
            .addToHistory()

        val summaryEvent = ContinuationSummaryEvent(
            data = ContentExport(
                "subby",
                null,
                ContentExport.MediaExport(
                    videoFile = null,
                    subtitles = listOf(
                        ContentExport.MediaExport.Subtitle(
                            subtitleFile = "subby.vtt",
                            language = "eng"
                        )
                    )
                ),
                null
            ),
            plan = ContentMigrationPlan(
                collection = "subby",
                videoContent = null,
                coverContent = null,
                subtitleContent = listOf(
                    ContentMigrationPlan.SingleSubtitle(
                        "eng",
                        "cached:///work/subby/subby.vtt",
                        "store:///subby/sub/eng/subby.vtt"
                    )
                )
            )
        ).derivedOf(collected)
            .addToHistory()

        val persistContent = PersistContentEvent()
            .derivedOf(summaryEvent)
            .addToHistory()

        val migratedEvent = MigrateContentToStoreTaskResultEvent(
            migrateData = MigrateContentToStoreTaskResultEvent.MigrateData(
                collection = "subby",
                videoMigrate = MigrateContentToStoreTaskResultEvent.FileMigration(null,
                    MigrateStatus.NotPresent),
                subtitleMigrate = emptyList(),
                coverMigrate = MigrateContentToStoreTaskResultEvent.FileMigration(null,
                    MigrateStatus.NotPresent),
            ),
            status =  TaskStatus.Completed
        ).derivedOf(persistContent)

        val result = listener.onEvent(migratedEvent, history)
        assertThat(result).isInstanceOf(StoreContentAndMetadataTaskCreatedEvent::class.java)

        val slot = slot<StoreContentAndMetadataTask>()

        verify(exactly = 1) {
            TaskStore.persist(capture(slot))
        }

        val storeTask = slot.captured
        assertThat(storeTask.data.collection).isEqualTo("subby")
        assertThat(storeTask.data.metadata).isNull()
        assertThat(storeTask.data.media?.videoFile).isNull()
        assertThat(storeTask.data.media?.subtitles?.first()?.subtitleFile).isEqualTo("subby.vtt")
        assertThat(storeTask.data.media?.subtitles?.first()?.language).isEqualTo("eng")

    }



    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private fun migrateEvent(
        status: TaskStatus = TaskStatus.Completed,
        collection: String = "TestCollection",
        videoUri: String? = null,
        coverUri: String? = null,
        subtitleUris: List<String> = emptyList()
    ): MigrateContentToStoreTaskResultEvent {
        return MigrateContentToStoreTaskResultEvent(
            status = status,
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

    class DummyEvent : Event()


}