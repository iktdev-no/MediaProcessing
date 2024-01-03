package no.iktdev.mediaprocessing.coordinator

import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.coordination.Tasks
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

abstract class TaskCreator: TaskCreatorListener {
    val log = KotlinLogging.logger {}
    abstract val producesEvent: KafkaEvents

    @Autowired
    lateinit var producer: CoordinatorProducer

    @Autowired
    lateinit var coordinator: Coordinator

    open val requiredEvents: List<KafkaEvents> = listOf()
    open val listensForEvents: List<KafkaEvents> = listOf()

    open fun isPrerequisiteEventsOk(events: List<PersistentMessage>): Boolean {
        val currentEvents = events.map { it.event }
        return requiredEvents.all { currentEvents.contains(it) }
    }
    open fun isPrerequisiteDataPresent(events: List<PersistentMessage>): Boolean {
        val failed = events.filter { e -> e.event in requiredEvents }.filter { !it.data.isSuccess() }
        return failed.isEmpty()
    }

    open fun isEventOfSingle(event: PersistentMessage, singleOne: KafkaEvents): Boolean {
        return event.event == singleOne
    }

    fun getListener(): Tasks {
        val eventListenerFilter = listensForEvents.ifEmpty { requiredEvents }
        return Tasks(taskHandler = this, producesEvent = producesEvent, listensForEvents = eventListenerFilter)
    }


    open fun prerequisitesRequired(events: List<PersistentMessage>): List<() -> Boolean> {
        return listOf {
            isPrerequisiteEventsOk(events)
        }
    }

    open fun prerequisiteRequired(event: PersistentMessage): List<() -> Boolean> {
        return listOf()
    }


    private val context: MutableMap<String, Any> = mutableMapOf()
    private val context_key_reference = "reference"
    private val context_key_producesEvent = "event"
    final override fun onEventReceived(referenceId: String, event: PersistentMessage, events: List<PersistentMessage>) {
        context[context_key_reference] = referenceId
        getListener().producesEvent.let {
            context[context_key_producesEvent] = it
        }

        if (prerequisitesRequired(events).all { it.invoke() } && prerequisiteRequired(event).all { it.invoke() }) {
            val result = onProcessEvents(event, events)
            if (result != null) {
                onResult(result)
            }
        } else {
            // TODO: Re-enable this
            // log.info { "Skipping: ${event.event} as it does not fulfill the requirements for ${context[context_key_producesEvent]}" }
        }
    }

    abstract fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper?


    private fun onResult(data: MessageDataWrapper) {
        producer.sendMessage(
            referenceId = context[context_key_reference] as String,
            event = context[context_key_producesEvent] as KafkaEvents,
            data = data
        )
    }

    @PostConstruct
    fun postConstruct() {
        coordinator.listeners.add(getListener())
    }
}

fun interface Prerequisite {
    fun execute(value: Any): Boolean
}

interface TaskCreatorListener {
    fun onEventReceived(referenceId: String, event: PersistentMessage,  events: List<PersistentMessage>): Unit
}