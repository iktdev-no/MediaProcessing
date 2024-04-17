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
class CreateEncodeWorkTask(@Autowired override var coordinator: Coordinator) : CreateProcesserWorkTask(coordinator) {
    val log = KotlinLogging.logger {}
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventWorkEncodeCreated

    override val requiredEvents: List<KafkaEvents>
        get() = listOf(KafkaEvents.EventMediaParameterEncodeCreated)

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        log.info { "${event.referenceId} triggered by ${event.event}" }
        return super.onProcessEvents(event, events)
    }

}