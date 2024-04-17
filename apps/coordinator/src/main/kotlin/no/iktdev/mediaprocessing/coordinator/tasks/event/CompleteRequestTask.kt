package no.iktdev.mediaprocessing.coordinator.tasks.event

import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.coordinator.mapping.ProcessMapping
import no.iktdev.mediaprocessing.shared.common.lastOrSuccessOf
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents.*
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessCompleted
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CompleteRequestTask(@Autowired override var coordinator: Coordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}

    override val producesEvent: KafkaEvents = KafkaEvents.EventRequestProcessCompleted

    override val requiredEvents: List<KafkaEvents> = listOf(
        EVENT_REQUEST_PROCESS_STARTED,
    )
    override val listensForEvents: List<KafkaEvents> = KafkaEvents.entries



    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        log.info { "${event.referenceId} triggered by ${event.event}" }

        val started = events.lastOrSuccessOf(EVENT_REQUEST_PROCESS_STARTED) ?: return null
        if (!started.data.isSuccess()) {
            return null
        }

        val receivedEvents = events.map { it.event }

        // TODO: Add filter in case a metadata request was performed or a cover download was performed. for now, for base functionality, it requires a performed event.

        val requiresOneOf = listOf(
            EventWorkConvertPerformed,
            EventWorkExtractPerformed,
            EventWorkEncodePerformed
        )

        if (requiresOneOf.none { it in receivedEvents }) {
            val missing = requiresOneOf.filter { !receivedEvents.contains(it) }
            log.info { "Can't complete at this moment. Missing required event(s)" + missing.joinToString("\n\t") }
            return null //SimpleMessageData(Status.SKIPPED, "Can't collect at this moment. Missing required event")
        }




        val mapper = ProcessMapping(events)
        if (mapper.canCollect()) {
            return ProcessCompleted(Status.COMPLETED, event.eventId)
        }
        return null
    }
}