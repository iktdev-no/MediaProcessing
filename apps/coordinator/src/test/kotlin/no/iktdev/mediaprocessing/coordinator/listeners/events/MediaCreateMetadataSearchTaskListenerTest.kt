package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.clearMocks
import io.mockk.slot
import io.mockk.verify
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.MockData.mediaParsedEvent
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MetadataSearchTask
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MediaCreateMetadataSearchTaskListenerTest : TestBase() {

    val listener = MediaCreateMetadataSearchTaskListener()

    @Test
    fun success1() {
        val started = defaultStartEvent()
        val parsedInfo = mediaParsedEvent("Baking Bread", "Baking Bread - S01E01 - Flour", MediaType.Serie)
            .derivedOf(started)

        val history = listOf(started, parsedInfo)
        val result = listener.onEvent(parsedInfo, history)
        assertThat(result is MetadataSearchTaskCreatedEvent)
        val slot = slot<MetadataSearchTask>()

        verify(exactly = 1) {
            TaskStore.persist(capture(slot))
        }

        val storeTask = slot.captured
        assertThat(storeTask.data.collection).isEqualTo("Baking Bread")
        assertThat(storeTask.data.searchTitles).hasSize(2)
        assertThat(storeTask.data.searchTitles).isEqualTo(
            listOf(
                "Baking Bread",
                "Baking Bread - S01E01 - Flour"
            )
        )
    }

    @DisplayName(
        """
        Hvis event ikke er MediaParsedInfoEvent
        Når onEvent kalles
        Så:
            Returneres null
        """
    )
    @Test
    fun ignored1() {
        // Hvis
        val listener = MediaCreateMetadataSearchTaskListener()
        val event = MetadataSearchResultEvent(
            status = TaskStatus.Completed
        )
        val history = emptyList<Event>()

        // Når
        val result = listener.onEvent(event, history)

        // Så
        assertThat(result).isNull()
        verify(exactly = 0) { TaskStore.persist(any()) }

    }

    @DisplayName(
        """
        Hvis event ikke er MediaParsedInfoEvent
        Når onEvent kalles
        Så:
            Returneres null
    """
    )
    @Test
    fun `hvis event ikke er MediaParsedInfoEvent saa returneres null`() {
        // Hvis
        val listener = MediaCreateMetadataSearchTaskListener()
        val event = MetadataSearchResultEvent(status = TaskStatus.Completed)
        val history = emptyList<Event>()

        // Når
        val result = listener.onEvent(event, history)

        // Så
        verify(exactly = 0) { TaskStore.persist(any()) }
        assertThat(result).isNull()
    }

    @DisplayName(
        """
        Hvis MediaParsedInfoEvent mottas
        Når onEvent kalles
        Så:
            Opprettes MetadataSearchTask
            Og timeout planlegges
    """
    )
    @Test
    fun `hvis parsed event saa opprettes task og timeout planlegges`() {
        // Hvis
        val listener = MediaCreateMetadataSearchTaskListener()
        val started = defaultStartEvent()
        val parsed = mediaParsedEvent(
            "Baking Bread",
            "Baking Bread - S01E01 - Flour",
            MediaType.Serie
        ).derivedOf(started)

        val history = listOf(started, parsed)

        // Når
        val result = listener.onEvent(parsed, history)

        // Så
        assertThat(result).isInstanceOf(MetadataSearchTaskCreatedEvent::class.java)

        val slot = slot<MetadataSearchTask>()
        verify(exactly = 1) { TaskStore.persist(capture(slot)) }

        val taskId = slot.captured.taskId
        assertThat(listener.scheduledExpiries).containsKey(taskId)
    }

    @DisplayName("""
        Hvis MetadataSearchTaskCreatedEvent re-spilles
        Og historikken inneholder MetadataSearchResultEvent for samme task
        Når onEvent kalles
        Så:
            Skal timeout ikke planlegges
    """)
    @Test
    fun `hvis replay og result finnes saa planlegges ikke timeout`() {
        // Hvis
        val listener = MediaCreateMetadataSearchTaskListener()

        val started = defaultStartEvent()
        val parsed = mediaParsedEvent(
            "Baking Bread",
            "Baking Bread - S01E01 - Flour",
            MediaType.Serie
        ).apply { derivedOf(started) }

        val task = MetadataSearchTask(
            MetadataSearchTask.SearchData(
                searchTitles = parsed.data.parsedSearchTitles,
                collection = parsed.data.parsedCollection
            )
        ).derivedOf(parsed)

        val created = MetadataSearchTaskCreatedEvent(task.taskId).derivedOf(parsed)

        val resultEvent = MetadataSearchResultEvent(
            status = TaskStatus.Completed,
            results = emptyList()
        ).producedFrom(task)

        val history = listOf(started, parsed, created, resultEvent)

        // Når
        listener.onEvent(created, history)

        // Så
        assertThat(listener.scheduledExpiries).doesNotContainKey(task.taskId)
    }

    @DisplayName("""
    Hvis flere MediaParsedInfoEvent mottas
    Når onEvent kalles for hver
    Så:
        Opprettes én MetadataSearchTask per event
""")
    @Test
    fun `hvis flere parsed events saa opprettes en task per event`() {
        // Hvis
        val listener = MediaCreateMetadataSearchTaskListener()

        val started = defaultStartEvent()

        val parsed1 = mediaParsedEvent("A", "A - E01", MediaType.Serie).derivedOf(started)
        val parsed2 = mediaParsedEvent("B", "B - E01", MediaType.Serie).derivedOf(started)

        // Når
        listener.onEvent(parsed1, listOf(started, parsed1))
        listener.onEvent(parsed2, listOf(started, parsed2))

        // Så
        verify(exactly = 2) { TaskStore.persist(any<MetadataSearchTask>()) }
    }

    @DisplayName(
        """
        Hvis MetadataSearchResultEvent mottas
        Når onEvent kalles
        Så:
            Timeout slettes
    """
    )
    @Test
    fun timeoutRemoval1() {
        // Hvis
        val listener = MediaCreateMetadataSearchTaskListener()
        val started = defaultStartEvent()
        val parsed = mediaParsedEvent(
            "Baking Bread",
            "Baking Bread - S01E01 - Flour",
            MediaType.Serie
        ).derivedOf(started)

        val history = listOf(started, parsed)

        // Når
        val result = listener.onEvent(parsed, history)

        // Så
        assertThat(result).isInstanceOf(MetadataSearchTaskCreatedEvent::class.java)

        val slot = slot<MetadataSearchTask>()
        verify(exactly = 1) { TaskStore.persist(capture(slot)) }

        val taskId = slot.captured.taskId
        assertThat(listener.scheduledExpiries).containsKey(taskId)

        // Completed Event here
        clearMocks(TaskStore, answers = false)

        val resultEvent = MetadataSearchResultEvent(
            status = TaskStatus.Completed,
            results = emptyList()
        ).producedFrom(slot.captured)

        val newHistory = history + listOf(resultEvent)
        listener.onEvent(resultEvent, newHistory)
        verify(exactly = 0) { TaskStore.persist(any()) }

        assertThat(listener.scheduledExpiries).doesNotContainKey(taskId)
    }



}