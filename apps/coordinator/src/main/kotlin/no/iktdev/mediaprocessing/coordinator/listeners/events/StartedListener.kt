package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.ListenerOrder
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import org.springframework.stereotype.Component

@ListenerOrder(1)
@Component
class StartedListener : EventListener() {
    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event as? FileReadyEvent ?: return null

        return StartProcessingEvent(
            data = StartData(
                flow = StartFlow.Auto,
                fileUri = useEvent.data.fileUri,
                operation = setOf(
                    OperationType.ExtractSubtitles,
                    OperationType.ConvertSubtitles,
                    OperationType.Encode
                )
            )
        )
    }
}