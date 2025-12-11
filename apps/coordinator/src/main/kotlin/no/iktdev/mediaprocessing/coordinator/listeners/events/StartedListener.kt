package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.FileReadyEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcessFlow
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Order(1)
@Component
class StartedListener : EventListener() {
    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event as? FileReadyEvent ?: return null

        return StartProcessingEvent(
            data = StartData(
                flow = ProcessFlow.Auto,
                fileUri = useEvent.data.fileUri,
                operation = setOf(
                    OperationType.Extract,
                    OperationType.Convert,
                    OperationType.Encode
                )
            )
        )
    }
}