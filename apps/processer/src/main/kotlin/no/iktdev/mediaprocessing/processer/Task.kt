package no.iktdev.mediaprocessing.processer

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import javax.annotation.PostConstruct

abstract class TaskCreator: TaskCreatorListener {
    private val log = KotlinLogging.logger {}

    @Autowired
    lateinit var producer: CoordinatorProducer

    @Autowired
    lateinit var coordinator: Coordinator

    @Autowired
    lateinit var socketMessage: SimpMessagingTemplate

    open val requiredEvents: List<KafkaEvents> = listOf()

    open fun isPrerequisiteEventsOk(events: List<PersistentProcessDataMessage>): Boolean {
        val currentEvents = events.map { it.event }
        return requiredEvents.all { currentEvents.contains(it) }
    }
    open fun isPrerequisiteDataPresent(events: List<PersistentProcessDataMessage>): Boolean {
        val failed = events.filter { e -> e.event in requiredEvents }.filter { !it.data.isSuccess() }
        return failed.isEmpty()
    }

    open fun isEventOfSingle(event: PersistentProcessDataMessage, singleOne: KafkaEvents): Boolean {
        return event.event == singleOne
    }

    abstract fun getListener(): Tasks

    open fun prerequisitesRequired(events: List<PersistentProcessDataMessage>): List<() -> Boolean> {
        return listOf {
            isPrerequisiteEventsOk(events)
        }
    }



    private val context: MutableMap<String, Any> = mutableMapOf()
    private val context_key_reference = "reference"
    private val context_key_producesEvent = "event"
    final override fun onEventReceived(referenceId: String, event: PersistentProcessDataMessage, events: List<PersistentProcessDataMessage>) {
        context[context_key_reference] = referenceId
        getListener().producesEvent.let {
            context[context_key_producesEvent] = it
        }

        if (prerequisitesRequired(events).all { it.invoke() }) {
            val result = onProcessEvents(event, events)
            if (result != null) {
                onResult(result)
            }
        } else {
            log.info { "Skipping: ${event.event} as it does not fulfill the requirements for ${context[context_key_producesEvent]}" }
        }
    }

    abstract fun onProcessEvents(event: PersistentProcessDataMessage, events: List<PersistentProcessDataMessage>): MessageDataWrapper?


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
    fun onEventReceived(referenceId: String, event: PersistentProcessDataMessage, events: List<PersistentProcessDataMessage>): Unit
}