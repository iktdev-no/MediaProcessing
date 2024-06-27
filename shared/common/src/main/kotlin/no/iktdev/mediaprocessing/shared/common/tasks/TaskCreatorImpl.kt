package no.iktdev.mediaprocessing.shared.common.tasks

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.EventCoordinatorBase
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

abstract class TaskCreatorImpl<C : EventCoordinatorBase<V, L>, V, L : EventBasedMessageListener<V>>(
    open var coordinator: C
) : ITaskCreatorListener<V> {
    private val log = KotlinLogging.logger {}

    protected open val processedEvents: MutableMap<String, Set<String>> = mutableMapOf()

    companion object {
        fun <T> isInstanceOfTaskCreatorImpl(clazz: Class<T>): Boolean {
            val superClass = TaskCreatorImpl::class.java
            return superClass.isAssignableFrom(clazz)
        }
    }

    // Event that the implementer sets
    abstract val producesEvent: KafkaEvents

    open val requiredEvents: List<KafkaEvents> = listOf()
    open val listensForEvents: List<KafkaEvents> = listOf()

    @Autowired
    lateinit var producer: CoordinatorProducer
    fun getListener(): Tasks<V> {
        val reactableEvents = (requiredEvents + listensForEvents).distinct()
        //val eventListenerFilter = listensForEvents.ifEmpty { requiredEvents }
        return Tasks(taskHandler = this, producesEvent = producesEvent, listensForEvents = reactableEvents)
    }
    @PostConstruct
    open fun attachListener() {
        coordinator.listeners.add(getListener())
    }


    /**
     *  Example implementation
     *
     *      open fun isPrerequisiteEventsOk(events: List<V>): Boolean {
     *          val currentEvents = events.map { it.event }
     *          return requiredEvents.all { currentEvents.contains(it) }
     *      }
     *
     */
    abstract fun isPrerequisiteEventsOk(events: List<V>): Boolean

    /**
     * Example implementation
     *
     *     open fun isPrerequisiteDataPresent(events: List<V>): Boolean {
     *         val failed = events
     *              .filter { e -> e.event in requiredEvents }
     *              .filter { !it.data.isSuccess() }
     *         return failed.isEmpty()
     *     }
     */
    abstract fun isPrerequisiteDataPresent(events: List<V>): Boolean

    /**
     * Example implementation
     *
     *     open fun isEventOfSingle(event: V, singleOne: KafkaEvents): Boolean {
     *         return event.event == singleOne
     *     }
     */
    abstract fun isEventOfSingle(event: V, singleOne: KafkaEvents): Boolean

    open fun prerequisitesRequired(events: List<V>): List<() -> Boolean> {
        return listOf {
            isPrerequisiteEventsOk(events)
        }
    }

    open fun prerequisiteRequired(event: V): List<() -> Boolean> {
        return listOf()
    }

    private val context: MutableMap<String, Any> = mutableMapOf()
    private val context_key_reference = "reference"
    private val context_key_producesEvent = "event"

    final override fun onEventReceived(referenceId: String, event: V, events: List<V>) {
        context[context_key_reference] = referenceId
        getListener().producesEvent.let {
            context[context_key_producesEvent] = it
        }

        if (prerequisitesRequired(events).all { it.invoke() } && prerequisiteRequired(event).all { it.invoke() }) {

            if (!containsUnprocessedEvents(events)) {
                log.warn { "Event register blocked proceeding" }
                return
            }

            val result = onProcessEvents(event, events)
            if (result != null) {
                onResult(result)
            }
        } else {
            // TODO: Re-enable this
            // log.info { "Skipping: ${event.event} as it does not fulfill the requirements for ${context[context_key_producesEvent]}" }
        }
    }

    /**
     * This function is intended to cache the referenceId and its eventid's
     * This is to prevent dupliation
     * */
    abstract fun containsUnprocessedEvents(events: List<V>): Boolean


    protected fun onResult(data: MessageDataWrapper) {

        producer.sendMessage(
            referenceId = context[context_key_reference] as String,
            event = context[context_key_producesEvent] as KafkaEvents,
            data = data
        )
    }

    abstract fun onProcessEvents(event: V, events: List<V>): MessageDataWrapper?

}