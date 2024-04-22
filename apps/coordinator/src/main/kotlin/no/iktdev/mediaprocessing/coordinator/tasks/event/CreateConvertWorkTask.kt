package no.iktdev.mediaprocessing.coordinator.tasks.event

import mu.KotlinLogging
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
    val log = KotlinLogging.logger {}
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventWorkConvertCreated

    override val requiredEvents: List<KafkaEvents>
        get() = listOf(
            KafkaEvents.EventWorkExtractCreated
            // TODO: Add event for request as well
        )

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        super.onProcessEventsAccepted(event, events)

        log.info { "${event.referenceId} @ ${event.eventId} triggered by ${event.event}" }

        // Check what it is and create based on it

        val derivedInfoObject = if (event.event in requiredEvents) {
            DerivedInfoObject.fromExtractWorkCreated(event)
        } else {
            val extractEvent = events.findLast { it.event == KafkaEvents.EventWorkExtractCreated }
            extractEvent?.let { it -> DerivedInfoObject.fromExtractWorkCreated(it) }
        } ?: return null


        val requiredEventId = if (event.event == KafkaEvents.EventWorkExtractCreated) {
            event.eventId
        } else null;

        val outFile = File(derivedInfoObject.outputFile)
        return ConvertWorkerRequest(
            status = Status.COMPLETED,
            requiresEventId = requiredEventId,
            inputFile = derivedInfoObject.outputFile,
            allowOverwrite = true,
            outFileBaseName = outFile.nameWithoutExtension,
            outDirectory = outFile.parentFile.absolutePath,
            derivedFromEventId = event.eventId
        )

    }

    private data class DerivedInfoObject(
        val outputFile: String,
        val derivedFromEventId: String,
        val requiresEventId: String
    ) {
        companion object {
            fun fromExtractWorkCreated(event: PersistentMessage): DerivedInfoObject? {
                return if (event.event != KafkaEvents.EventWorkExtractCreated) null else {
                    val data: FfmpegWorkRequestCreated = event.data as FfmpegWorkRequestCreated
                    DerivedInfoObject(
                        outputFile = data.outFile,
                        derivedFromEventId = event.eventId,
                        requiresEventId = event.eventId
                    )
                }
            }
        }
    }
}