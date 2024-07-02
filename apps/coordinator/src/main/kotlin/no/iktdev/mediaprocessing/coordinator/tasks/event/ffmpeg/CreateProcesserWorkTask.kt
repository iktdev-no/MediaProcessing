package no.iktdev.mediaprocessing.coordinator.tasks.event.ffmpeg

import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkerArgumentsCreated
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted

abstract class CreateProcesserWorkTask(override var coordinator: EventCoordinator) : TaskCreator(coordinator) {
    private val log = KotlinLogging.logger {}

    open fun isPermittedToCreateTasks(events: List<PersistentMessage>): Boolean {
        val event = events.firstOrNull() ?: return false
        val started = events.findLast { it.event == KafkaEvents.EventMediaProcessStarted }?.data as MediaProcessStarted?
        if (started == null) {
            log.info { "${event.referenceId} couldn't find start event" }
            return false
        } else if (started.type == ProcessType.MANUAL) {
            val proceed = events.find { it.event == KafkaEvents.EventMediaWorkProceedPermitted }
            if (proceed == null) {
                log.warn { "${event.referenceId} waiting for Proceed event due to Manual process" }
                return false
            } else {
                log.warn { "${event.referenceId} registered proceed permitted" }
            }
        }
        return true
    }




    fun createMessagesByArgs(event: PersistentMessage): List<MessageDataWrapper> {
        val events: MutableList<MessageDataWrapper> = mutableListOf()
        val earg = if (event.data is FfmpegWorkerArgumentsCreated) event.data as FfmpegWorkerArgumentsCreated? else return events
        if (earg == null || earg.entries.isEmpty()) {
            log.info { "${event.referenceId} ffargument is empty" }
            return events
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
            events.add(it)
        }
        return events
    }

}