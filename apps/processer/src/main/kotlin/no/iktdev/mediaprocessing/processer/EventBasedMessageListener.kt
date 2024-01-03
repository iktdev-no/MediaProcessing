package no.iktdev.mediaprocessing.processer

import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents

class EventBasedMessageListener {
    private val listeners: MutableList<Tasks> = mutableListOf()

    fun add(produces: KafkaEvents, listener: TaskCreatorListener) {
        listeners.add(Tasks(produces, listener))
    }

    fun add(task: Tasks) {
        listeners.add(task)
    }

    private fun waitingListeners(events: List<PersistentProcessDataMessage>): List<Tasks> {
        val nonCreators = listeners.filter { !events.map { e -> e.event }.contains(it.producesEvent) }
        return nonCreators
    }

    fun forwardEventMessageToListeners(newEvent: PersistentProcessDataMessage, events: List<PersistentProcessDataMessage>) {
        waitingListeners(events).forEach {
            try {
                it.taskHandler.onEventReceived(newEvent.referenceId, newEvent, events)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}

data class Tasks(
    val producesEvent: KafkaEvents,
    val taskHandler: TaskCreatorListener
)