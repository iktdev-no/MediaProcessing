package no.iktdev.mediaprocessing.ui.coordinator

import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.tasks.EventBasedMessageListener
import no.iktdev.mediaprocessing.shared.common.tasks.ITaskCreatorListener
import no.iktdev.mediaprocessing.shared.common.tasks.Tasks
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess

class PersistentEventBasedMessageListener: EventBasedMessageListener<PersistentMessage>() {

    override fun listenerWantingEvent(
        event: PersistentMessage,
        waitingListeners: List<Tasks<PersistentMessage>>
    ): List<Tasks<PersistentMessage>> {
        return waitingListeners.filter { event.event in it.listensForEvents }
    }

    override fun onForward(
        event: PersistentMessage,
        history: List<PersistentMessage>,
        listeners: List<ITaskCreatorListener<PersistentMessage>>
    ) {
        listeners.forEach {
            it.onEventReceived(referenceId = event.referenceId, event = event, events = history)
        }
    }

    override fun waitingListeners(events: List<PersistentMessage>): List<Tasks<PersistentMessage>> {
        val nonCreators = listeners.filter { !events.map { e -> e.event }.contains(it.producesEvent) }
        return nonCreators
    }

}