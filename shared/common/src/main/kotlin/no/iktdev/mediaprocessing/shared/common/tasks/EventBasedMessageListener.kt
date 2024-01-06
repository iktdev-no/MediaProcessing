package no.iktdev.mediaprocessing.shared.common.tasks

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents

abstract class EventBasedMessageListener<V> {
    val listeners: MutableList<Tasks<V>> = mutableListOf()

    fun add(produces: KafkaEvents, listener: ITaskCreatorListener<V>) {
        listeners.add(Tasks(producesEvent = produces, taskHandler =  listener))
    }

    fun add(task: Tasks<V>) {
        listeners.add(task)
    }

    /**
     * Example implementation
     *
     *     fun waitingListeners(events: List<PersistentMessage>): List<Tasks> {
     *         val nonCreators = listeners
     *              .filter { !events.map { e -> e.event }
     *                  .contains(it.producesEvent) }
     *         return nonCreators
     *     }
     */
    abstract fun waitingListeners(events: List<V>): List<Tasks<V>>

    /**
     *  Example implementation
     *
     *     fun listenerWantingEvent(event: PersistentMessage, waitingListeners: List<Tasks>)
     *          : List<Tasks>
     *     {
     *         return waitingListeners.filter { event.event in it.listensForEvents }
     *     }
     */
    abstract fun listenerWantingEvent(event: V, waitingListeners: List<Tasks<V>>): List<Tasks<V>>

    /**
     * Send to taskHandler
     */
    abstract fun onForward(event: V, history: List<V>, listeners: List<ITaskCreatorListener<V>>)

    /**
     * This will be called in sequence, thus some messages might be made a duplicate of.
     */
    fun forwardEventMessageToListeners(newEvent: V, events: List<V>) {
        val waitingListeners = waitingListeners(events)
        val availableListeners = listenerWantingEvent(event = newEvent, waitingListeners = waitingListeners)
        onForward(event = newEvent, history = events, listeners = availableListeners.map { it.taskHandler })
    }

    /**
     * This will be called with all messages at once, thus it should reflect kafka topic and database
     */
    fun forwardBatchEventMessagesToListeners(events: List<V>) {
        val waitingListeners = waitingListeners(events)
        onForward(event = events.last(), history = events, waitingListeners.map { it.taskHandler })
    }

}

data class Tasks<V>(
    val producesEvent: KafkaEvents,
    val listensForEvents: List<KafkaEvents> = listOf(),
    val taskHandler: ITaskCreatorListener<V>
)