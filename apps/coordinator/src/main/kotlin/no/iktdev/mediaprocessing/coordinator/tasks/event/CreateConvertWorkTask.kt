package no.iktdev.mediaprocessing.coordinator.tasks.event

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.coordinator.taskManager
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.persistance.isOfEvent
import no.iktdev.mediaprocessing.shared.common.persistance.isSuccess
import no.iktdev.mediaprocessing.shared.common.persistance.lastOf
import no.iktdev.mediaprocessing.shared.common.task.ConvertTaskData
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.contract.dto.StartOperationEvents
import no.iktdev.mediaprocessing.shared.contract.dto.isOnly
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.az
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ConvertWorkerRequest
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.work.ProcesserExtractWorkPerformed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class CreateConvertWorkTask(@Autowired override var coordinator: EventCoordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventWorkConvertCreated

    override val listensForEvents: List<KafkaEvents>
        get() = listOf(KafkaEvents.EventMediaProcessStarted, KafkaEvents.EventWorkExtractPerformed)

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        super.onProcessEventsAccepted(event, events)
        val startedEventData = events.lastOf(KafkaEvents.EventMediaProcessStarted)?.data?.az<MediaProcessStarted>()

        val result = if (event.isOfEvent(KafkaEvents.EventMediaProcessStarted) &&
            event.data.az<MediaProcessStarted>()?.operations?.isOnly(StartOperationEvents.CONVERT) == true
        ) {
            startedEventData?.file
        } else if (event.isOfEvent(KafkaEvents.EventWorkExtractPerformed) && startedEventData?.operations?.contains(
                StartOperationEvents.CONVERT
            ) == true
        ) {
            val innerData = event.data.az<ProcesserExtractWorkPerformed>()
            innerData?.outFile
        } else null

        val convertFile = result?.let { File(it) } ?: return null

        val taskData = ConvertTaskData(
            allowOverwrite = true,
            inputFile = convertFile.absolutePath,
            outFileBaseName = convertFile.nameWithoutExtension,
            outDirectory = convertFile.parentFile.absolutePath,
            outFormats = emptyList()
        )

        taskManager.createTask(
            referenceId = event.referenceId,
            eventId = event.eventId,
            task = TaskType.Convert,
            data = Gson().toJson(taskData)
        )

        return if (event.isOfEvent(KafkaEvents.EventMediaProcessStarted) &&
            event.data.az<MediaProcessStarted>()?.operations?.isOnly(StartOperationEvents.CONVERT) == true
        ) {
            produceConvertWorkRequest(convertFile, null, event.eventId)
        } else if (event.isOfEvent(KafkaEvents.EventWorkExtractPerformed) && startedEventData?.operations?.contains(
                StartOperationEvents.CONVERT
            ) == true
        ) {
            return produceConvertWorkRequest(convertFile, event.referenceId, event.eventId)

        } else null
    }

    private fun produceConvertWorkRequest(
        file: File,
        requiresEventId: String?,
        derivedFromEventId: String?
    ): ConvertWorkerRequest {
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