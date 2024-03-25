package no.iktdev.mediaprocessing.coordinator.tasks.event

import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.tasks.event.ffmpeg.CreateProcesserWorkTask
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateExtractWorkTask(@Autowired override var coordinator: Coordinator) : CreateProcesserWorkTask(coordinator) {
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EVENT_WORK_EXTRACT_CREATED

    override val requiredEvents: List<KafkaEvents>
        get() = listOf(KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED)
}