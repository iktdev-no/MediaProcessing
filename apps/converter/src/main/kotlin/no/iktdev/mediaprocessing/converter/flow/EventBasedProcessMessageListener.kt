package no.iktdev.mediaprocessing.converter.flow

import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.common.tasks.EventBasedMessageListener
import no.iktdev.mediaprocessing.shared.common.tasks.ITaskCreatorListener
import no.iktdev.mediaprocessing.shared.common.tasks.Tasks

class EventBasedProcessMessageListener: EventBasedMessageListener<PersistentProcessDataMessage>() {
    override fun waitingListeners(events: List<PersistentProcessDataMessage>): List<Tasks<PersistentProcessDataMessage>> {
        val nonCreators = listeners
            .filter { !events.map { e -> e.event }
                .contains(it.producesEvent) }
        return nonCreators
    }

    override fun listenerWantingEvent(event: PersistentProcessDataMessage, waitingListeners: List<Tasks<PersistentProcessDataMessage>>): List<Tasks<PersistentProcessDataMessage>> {
        return waitingListeners.filter { event.event in it.listensForEvents }
    }

    override fun onForward(
        event: PersistentProcessDataMessage,
        history: List<PersistentProcessDataMessage>,
        listeners: List<ITaskCreatorListener<PersistentProcessDataMessage>>
    ) {
        listeners.forEach {
            it.onEventReceived(referenceId = event.referenceId, event = event, events = history)
        }
    }

}