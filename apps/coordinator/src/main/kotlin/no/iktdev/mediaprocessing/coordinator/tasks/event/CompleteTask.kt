package no.iktdev.mediaprocessing.coordinator.tasks.event

import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.coordinator.mapping.ProcessMapping
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
class CompleteTask(@Autowired override var coordinator: Coordinator) : TaskCreator(coordinator) {
    override val producesEvent: KafkaEvents = KafkaEvents.EVENT_PROCESS_COMPLETED

    override val requiredEvents: List<KafkaEvents> = listOf(
        EVENT_PROCESS_STARTED,
        EVENT_MEDIA_READ_BASE_INFO_PERFORMED,
        EVENT_MEDIA_READ_OUT_NAME_AND_TYPE
    )
    override val listensForEvents: List<KafkaEvents> = KafkaEvents.entries



    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        val started = events.find { it.event == KafkaEvents.EVENT_PROCESS_STARTED } ?: return null
        if (!started.data.isSuccess()) {
            return null
        }

        val receivedEvents = events.map { it.event }

        val requiresOneOf = listOf(
            EVENT_MEDIA_EXTRACT_PARAMETER_CREATED,
            EVENT_MEDIA_ENCODE_PARAMETER_CREATED,
            EVENT_MEDIA_DOWNLOAD_COVER_PARAMETER_CREATED,
            EVENT_WORK_CONVERT_CREATED
        )

        if (!requiresOneOf.any { it in receivedEvents }) {
            log.info { "Can't complete at this moment. Missing required event" }
            return null //SimpleMessageData(Status.SKIPPED, "Can't collect at this moment. Missing required event")
        }




        val mapper = ProcessMapping(events)
        if (mapper.canCollect()) {
            return ProcessCompleted(Status.COMPLETED)
        }
        return null
    }
}