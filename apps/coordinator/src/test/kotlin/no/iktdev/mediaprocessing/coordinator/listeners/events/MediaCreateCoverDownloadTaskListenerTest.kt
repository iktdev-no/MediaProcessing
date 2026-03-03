package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.slot
import io.mockk.verify
import no.iktdev.mediaprocessing.MockData.mediaParsedEvent
import no.iktdev.mediaprocessing.MockData.metadataEvent
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.defaultMediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.CoverDownloadTask
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class MediaCreateCoverDownloadTaskListenerTest: TestBase() {

    val listener = MediaCreateCoverDownloadTaskListener()

    @Test
    fun success1() {
        val started = defaultStartEvent()
            .addToHistory()

        val parsed = mediaParsedEvent("Baking Bread", "Baking Bread - S01E01 - Flour", MediaType.Serie)
            .derivedOf(started)
            .addToHistory()

        val metadata = metadataEvent(started, coverUrl = "http://example.com/fancy.jpg")

        val result = listener.onEvent(metadata.last(), history)
        assertThat(result is CoverDownloadTaskCreatedEvent)
        val slot = slot<CoverDownloadTask>()

        verify(exactly = 1) {
            TaskStore.persist(capture(slot))
        }

        val storeTask = slot.captured
        assertThat(storeTask.data.url).isEqualTo("http://example.com/fancy.jpg")
        assertThat(storeTask.data.outputFileName).isEqualTo("MyCollection-potetland")
    }

}