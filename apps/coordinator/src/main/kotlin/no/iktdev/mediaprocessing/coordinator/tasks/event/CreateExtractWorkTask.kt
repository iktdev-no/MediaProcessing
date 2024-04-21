package no.iktdev.mediaprocessing.coordinator.tasks.event

import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.tasks.event.ffmpeg.CreateProcesserWorkTask
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateExtractWorkTask(@Autowired override var coordinator: Coordinator) : CreateProcesserWorkTask(coordinator) {
    val log = KotlinLogging.logger {}
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventWorkExtractCreated

    override val requiredEvents: List<KafkaEvents>
        get() = listOf(KafkaEvents.EventMediaParameterExtractCreated)

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        super.onProcessEvents(event, events)

        log.info { "${event.referenceId} triggered by ${event.event}" }

        val forwardEvent = if (event.event != KafkaEvents.EventMediaParameterExtractCreated) {
            val sevent = events.findLast { it.event == KafkaEvents.EventMediaParameterExtractCreated }
            if (sevent != null) {
                log.info { "${event.referenceId} ${event.event} is not of ${KafkaEvents.EventMediaParameterExtractCreated}, swapping to found event" }
            } else {
                log.info { "${event.referenceId} ${event.event} is not of ${KafkaEvents.EventMediaParameterExtractCreated}, could not find required event.." }
            }
            sevent ?: event
        } else event

        return super.onProcessEvents(forwardEvent, events)
    }
}