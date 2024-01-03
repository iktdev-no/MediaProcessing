package no.iktdev.mediaprocessing.coordinator.coordination

import no.iktdev.mediaprocessing.coordinator.TaskCreatorListener
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents

class EventBasedMessageListener {
    private val listeners: MutableList<Tasks> = mutableListOf()

    fun add(produces: KafkaEvents, listener: TaskCreatorListener) {
        listeners.add(Tasks(producesEvent = produces, taskHandler =  listener))
    }

    fun add(task: Tasks) {
        listeners.add(task)
    }

    private fun waitingListeners(events: List<PersistentMessage>): List<Tasks> {
        val nonCreators = listeners.filter { !events.map { e -> e.event }.contains(it.producesEvent) }
        return nonCreators
    }

    private fun listenerWantingEvent(event: PersistentMessage, waitingListeners: List<Tasks>): List<Tasks> {
        return waitingListeners.filter { event.event in it.listensForEvents }
    }

    /**
     * This will be called in sequence, thus some messages might be made a duplicate of.
     */
    fun forwardEventMessageToListeners(newEvent: PersistentMessage, events: List<PersistentMessage>) {
        val waitingListeners = waitingListeners(events)
        val availableListeners = listenerWantingEvent(event = newEvent, waitingListeners = waitingListeners)
        availableListeners.forEach {
            try {
                it.taskHandler.onEventReceived(newEvent.referenceId, newEvent, events)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * This will be called with all messages at once, thus it should reflect kafka topic and database
     */
    fun forwardBatchEventMessagesToListeners(events: List<PersistentMessage>) {
        val waitingListeners = waitingListeners(events)
        waitingListeners.forEach {
            try {
                val last = events.last()
                it.taskHandler.onEventReceived(last.referenceId, last, events)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}

data class Tasks(
    val producesEvent: KafkaEvents,
    val listensForEvents: List<KafkaEvents> = listOf(),
    val taskHandler: TaskCreatorListener
)