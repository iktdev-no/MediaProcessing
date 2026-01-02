package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoordinatorReadStreamsTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MediaReadStreamsTaskCreatedListenerTest: TestBase() {

    private val listener = MediaReadStreamsTaskCreatedListener()

    @Test
    @DisplayName("""
        Hvis event ikke er MediaParsedInfoEvent
        Når onEvent kalles
        Så:
            Returneres null
    """)
    fun testOnEventNonParsedInfoEvent() {
        val result = listener.onEvent(DummyEvent(), emptyList())
        assertNull(result)
    }

    @Test
    @DisplayName("""
        Hvis event er MediaParsedInfoEvent men history mangler StartProcessingEvent
        Når onEvent kalles
        Så:
            Returneres null
    """)
    fun testOnEventParsedInfoEventWithoutStartProcessing() {
        val parsedEvent = MediaParsedInfoEvent(
            MediaParsedInfoEvent.ParsedData(
                parsedCollection = "collection",
                parsedFileName = "file.mkv",
                parsedSearchTitles = listOf("title"),
                mediaType = MediaType.Movie
            )
        )
        val result = listener.onEvent(parsedEvent, emptyList())
        assertNull(result)
    }

    @Test
    @DisplayName("""
        Hvis event er MediaParsedInfoEvent og history inneholder StartProcessingEvent
        Når onEvent kalles
        Så:
            Returneres CoordinatorReadStreamsTaskCreatedEvent med riktig taskId
    """)
    fun testOnEventParsedInfoEventWithStartProcessing() {
        val parsedEvent = MediaParsedInfoEvent(
            MediaParsedInfoEvent.ParsedData(
                parsedCollection = "collection",
                parsedFileName = "file.mkv",
                parsedSearchTitles = listOf("title"),
                mediaType = MediaType.Movie
            )
        )
        val startEvent = StartProcessingEvent(StartData(fileUri = "file://test.mkv", operation = emptySet()))

        val result = listener.onEvent(parsedEvent, listOf(startEvent))

        assertNotNull(result)
        assertTrue(result is CoordinatorReadStreamsTaskCreatedEvent)

        val coordinatorEvent = result as CoordinatorReadStreamsTaskCreatedEvent
        assertNotNull(coordinatorEvent.taskId)
    }
}