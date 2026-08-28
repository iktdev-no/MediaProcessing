package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.slot
import io.mockk.verify
import no.iktdev.eventi.events.SoftDispatchException
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
import org.junit.jupiter.api.assertThrows

class MediaCreateMetadataSearchTaskListenerTest : TestBase() {

    private val listener = MediaCreateMetadataSearchTaskListener()

    @DisplayName(
        """
        Hvis MediaParsedInfoEvent mottas
        Når onEvent kalles
        Så:
            Opprettes MetadataSearchTask
            Og MetadataSearchTaskCreatedEvent returneres
        """
    )
    @Test
    fun `hvis parsed event mottas saa opprettes metadata search task`() {
        // Hvis
        val started = defaultStartEvent()

        val parsedInfo = mediaParsedEvent(
            "Baking Bread",
            "Baking Bread - S01E01 - Flour",
            MediaType.Serie
        ).derivedOf(started)

        val history = listOf(
            started,
            parsedInfo
        )

        // Når
        val result = listener.onEvent(parsedInfo, history)

        // Så
        assertThat(result)
            .isInstanceOf(MetadataSearchTaskCreatedEvent::class.java)

        val taskSlot = slot<MetadataSearchTask>()

        verify(exactly = 1) {
            TaskStore.persist(capture(taskSlot))
        }

        val task = taskSlot.captured

        assertThat(task.data.collection)
            .isEqualTo("Baking Bread")

        assertThat(task.data.searchTitles)
            .containsExactly(
                "Baking Bread",
                "Baking Bread - S01E01 - Flour"
            )
    }

    @DisplayName(
        """
        Hvis nødvendig historikk mangler
        Når onEvent kalles
        Så:
            Kastes MissingEventException
        """
    )
    @Test
    fun `hvis nødvendig historikk mangler saa kastes exception`() {
        // Hvis
        val event = MetadataSearchResultEvent(
            status = TaskStatus.Completed
        )

        val history = emptyList<Event>()

        // Når / Så
        assertThrows<SoftDispatchException.MissingEventException> {
            listener.onEvent(event, history)
        }
    }

    @DisplayName(
        """
        Hvis flere MediaParsedInfoEvent mottas
        Når onEvent kalles for hver
        Så:
            Opprettes én MetadataSearchTask per event
        """
    )
    @Test
    fun `hvis flere parsed events mottas saa opprettes en task per event`() {
        // Hvis
        val started = defaultStartEvent()

        val parsed1 = mediaParsedEvent(
            "A",
            "A - E01",
            MediaType.Serie
        ).derivedOf(started)

        val parsed2 = mediaParsedEvent(
            "B",
            "B - E01",
            MediaType.Serie
        ).derivedOf(started)

        // Når
        listener.onEvent(
            parsed1,
            listOf(started, parsed1)
        )

        listener.onEvent(
            parsed2,
            listOf(started, parsed2)
        )

        // Så
        verify(exactly = 2) {
            TaskStore.persist(any<MetadataSearchTask>())
        }
    }
}