package no.iktdev.mediaprocessing.coordinator.tasks.event

import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ConvertWorkerRequest
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class CreateConvertWorkTask(@Autowired override var coordinator: Coordinator) : TaskCreator(coordinator) {
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventWorkConvertCreated

    override val requiredEvents: List<KafkaEvents>
        get() = listOf(
            KafkaEvents.EventWorkExtractCreated
            // TODO: Add event for request as well
        )

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        if (!event.data.isSuccess()) {
            return null
        }
        val eventData = event.data as FfmpegWorkRequestCreated? ?: return null

        val requiredEventId = if (event.event == KafkaEvents.EventWorkExtractCreated) {
            event.eventId
        } else null;

        val outFile = File(eventData.outFile)
        return ConvertWorkerRequest(
            status = Status.COMPLETED,
            requiresEventId = requiredEventId,
            inputFile = eventData.outFile,
            allowOverwrite = true,
            outFileBaseName = outFile.nameWithoutExtension,
            outDirectory = outFile.parentFile.absolutePath,
            derivedFromEventId = event.eventId
        )

    }
}