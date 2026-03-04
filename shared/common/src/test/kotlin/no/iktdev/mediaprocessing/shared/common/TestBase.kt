package no.iktdev.mediaprocessing.shared.common

import io.mockk.*
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.registry.EventTypeRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.util.*

open class TestBase {

    val history = mutableListOf<Event>()

    class DummyEvent: Event()
    class DummyTask: Task()


    @BeforeEach
    open fun setup() {
        history.clear()
    }


    fun defaultStartEvent(): StartProcessingEvent {
        val start = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.Encode, OperationType.ExtractSubtitles, OperationType.ConvertSubtitles, OperationType.MetadataSearch),
                fileUri = "file:///unit/${UUID.randomUUID()}.mkv"
            )
        ).apply { newReferenceId() }
        return start

    }

    fun Event.addToHistory(): Event {
        history.add(this)
        return this
    }

}