package no.iktdev.mediaprocessing.coordinator.tasks.event.ffmpeg

import mu.KotlinLogging
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

abstract class CreateProcesserWorkTask(override var coordinator: Coordinator) : TaskCreator(coordinator) {
    private val log = KotlinLogging.logger {}
    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        val started = events.findLast { it.event == KafkaEvents.EventMediaProcessStarted }?.data as MediaProcessStarted?
        if (started == null) {
            log.info { "${event.referenceId} couldn't find start event" }
            return null
        } else if (started.type == ProcessType.MANUAL) {
            val proceed = events.find { it.event == KafkaEvents.EventMediaWorkProceedPermitted }
            if (proceed == null) {
                log.warn { "${event.referenceId} waiting for Proceed event due to Manual process" }
                return null
            } else {
                log.warn { "${event.referenceId} registered proceed permitted" }
            }
        }


        val earg = if (event.data is FfmpegWorkerArgumentsCreated) event.data as FfmpegWorkerArgumentsCreated? else return null
        if (earg == null || earg.entries.isEmpty()) {
            log.info { "${event.referenceId} ffargument is empty" }
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
            log.info { "${event.referenceId} creating work request based on ${it.derivedFromEventId}" }
            super.onResult(it)
        }
        return null
    }
}