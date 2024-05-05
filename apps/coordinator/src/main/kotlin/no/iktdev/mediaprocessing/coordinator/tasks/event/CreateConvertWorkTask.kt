package no.iktdev.mediaprocessing.coordinator.tasks.event

import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.persistance.lastOf
import no.iktdev.mediaprocessing.shared.contract.dto.StartOperationEvents
import no.iktdev.mediaprocessing.shared.contract.dto.isOnly
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.az
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ConvertWorkerRequest
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted
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
        )
    override val listensForEvents: List<KafkaEvents>
        get() = listOf(KafkaEvents.EventMediaProcessStarted)

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        super.onProcessEventsAccepted(event, events)

        log.info { "${event.referenceId} @ ${event.eventId} triggered by ${event.event}" }
        val startedEvent = events.lastOf(KafkaEvents.EventMediaProcessStarted)
        val startedEventData = events.lastOf(KafkaEvents.EventMediaProcessStarted)?.data?.az<MediaProcessStarted>()
        if (startedEventData?.operations?.isOnly(StartOperationEvents.CONVERT) == true) {
            val subtitleFile = File(startedEventData.file)
            return produceConvertWorkRequest(subtitleFile, null, startedEvent?.eventId)
        } else {
            val derivedInfoObject = if (event.event in requiredEvents) {
                DerivedInfoObject.fromExtractWorkCreated(event)
            } else {
                val extractEvent = events.lastOf(KafkaEvents.EventWorkExtractCreated)
                extractEvent?.let { it -> DerivedInfoObject.fromExtractWorkCreated(it) }
            } ?: return null


            val requiredEventId = if (event.event == KafkaEvents.EventWorkExtractCreated) {
                event.eventId
            } else null;

            val outFile = File(derivedInfoObject.outputFile)
            return produceConvertWorkRequest(outFile, requiredEventId, event.eventId)
        }
    }

    private fun produceConvertWorkRequest(file: File, requiresEventId: String?, derivedFromEventId: String?): ConvertWorkerRequest {
        return ConvertWorkerRequest(
            status = Status.COMPLETED,
            requiresEventId = requiresEventId,
            inputFile = file.absolutePath,
            allowOverwrite = true,
            outFileBaseName = file.nameWithoutExtension,
            outDirectory = file.parentFile.absolutePath,
            derivedFromEventId = derivedFromEventId
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