package no.iktdev.mediaprocessing.coordinator.tasks.event.ffmpeg

import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.coordinator.log
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkerArgumentsCreated
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import org.springframework.beans.factory.annotation.Autowired

abstract class CreateProcesserWorkTask(override var coordinator: Coordinator) : TaskCreator(coordinator) {

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        val started = events.findLast { it.event == KafkaEvents.EVENT_MEDIA_PROCESS_STARTED }?.data as MediaProcessStarted?
        if (started == null) {
            return null
        }

        if (!event.data.isSuccess()) {
            return null
        }

        val proceed = events.find { it.event == KafkaEvents.EVENT_MEDIA_WORK_PROCEED_PERMITTED }
        if (proceed == null && started.type == ProcessType.MANUAL) {
            log.warn { "${event.referenceId} waiting for Proceed event due to Manual process" }
            return null
        }


        val earg = if (event.data is FfmpegWorkerArgumentsCreated) event.data as FfmpegWorkerArgumentsCreated? else return null
        if (earg == null || earg.entries.isEmpty()) {
            return null
        }

        val requestEvents = earg.entries.map {
            FfmpegWorkRequestCreated(
                status = Status.COMPLETED,
                derivedFromEventId = event.eventId,
                inputFile = earg.inputFile,
                arguments = it.arguments,
                outFile = it.outputFile
            )
        }
        requestEvents.forEach {
            super.onResult(it)
        }
        return null
    }
}