package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.slot
import io.mockk.verify
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreContentAndMetadataTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.StoreContentAndMetadataTask
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.common.model.MigrateStatus
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

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
        val startedEvent = defaultStartEvent()
        val event = DummyEvent().derivedOf(startedEvent)
        val history = emptyList<Event>()

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
        val startedEvent = defaultStartEvent()
        val event = migrateEvent().derivedOf(startedEvent)
        val history = listOf(DummyEvent().derivedOf(startedEvent))

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

        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "MyCollection",
                parsedFileName = "MyCollection",
                parsedSearchTitles = listOf("MyCollection"),
                mediaType = MediaType.Serie,
                episodeInfo = null
            )
        ).derivedOf(startedEvent)

        val migrate = migrateEvent(
            status = TaskStatus.Completed,
            collection = "Baking Bread",
            videoUri = "file:///Baking Bread/Baking Bread - S01E01 - Flour.mp4",
            coverUri = "file:///Baking Bread/Baking Bread.jpg",
            subtitleUris = listOf("file:///Baking Bread/en/Baking Bread - S01E01 - Flour.srt")
        ).derivedOf(parsed)

        val collected = CollectedEvent(setOf(startedEvent.eventId, parsed.eventId))
            .derivedOf(migrate)

        val history = listOf(
            startedEvent,
            parsed,
            collected,
            migrate
        )

        val result = listener.onEvent(migrate, history)
        assertThat(result).isInstanceOf(StoreContentAndMetadataTaskCreatedEvent::class.java)

        val slot = slot<StoreContentAndMetadataTask>()

        verify(exactly = 1) {
            TaskStore.persist(capture(slot))
        }

        val storeTask = slot.captured
        assertThat(storeTask.data.collection).isEqualTo("Baking Bread")
        assertThat(storeTask.data.metadata.mediaType).isEqualTo(MediaType.Serie)
        assertThat(storeTask.data.media?.videoFile).isEqualTo("Baking Bread - S01E01 - Flour.mp4")
        assertThat(storeTask.data.media?.subtitles?.first()?.subtitleFile).isEqualTo("Baking Bread - S01E01 - Flour.srt")
        assertThat(storeTask.data.media?.subtitles?.first()?.language).isEqualTo("en")

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
            coverMigrate = listOfNotNull(
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