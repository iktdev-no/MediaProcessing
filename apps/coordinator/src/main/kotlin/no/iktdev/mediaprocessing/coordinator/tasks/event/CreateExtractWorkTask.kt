package no.iktdev.mediaprocessing.coordinator.tasks.event

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.taskManager
import no.iktdev.mediaprocessing.coordinator.tasks.event.ffmpeg.CreateProcesserWorkTask
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.persistance.isOfEvent
import no.iktdev.mediaprocessing.shared.common.persistance.isSuccess
import no.iktdev.mediaprocessing.shared.common.task.FfmpegTaskData
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.az
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkerArgumentsCreated
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class CreateExtractWorkTask(@Autowired override var coordinator: EventCoordinator) : CreateProcesserWorkTask(coordinator) {
    val log = KotlinLogging.logger {}
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventWorkExtractCreated

    override val requiredEvents: List<KafkaEvents>
        get() = listOf(KafkaEvents.EventMediaParameterExtractCreated)

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        super.onProcessEventsAccepted(event, events)
        log.info { "${event.referenceId} triggered by ${event.event}" }


        if (events.lastOrNull { it.isOfEvent(KafkaEvents.EventMediaParameterExtractCreated) }?.isSuccess() != true) {
            log.warn { "Last instance of ${KafkaEvents.EventMediaParameterExtractCreated} was unsuccessful or null. Skipping.." }
            return null
        }

        if (!isPermittedToCreateTasks(events)) {
            log.warn { "Cannot continue until permitted event is present" }
        }

        val forwardEvent = if (event.event != KafkaEvents.EventMediaParameterExtractCreated) {
            val sevent = events.findLast { it.event == KafkaEvents.EventMediaParameterExtractCreated }
            if (sevent != null) {
                log.info { "${event.referenceId} ${event.event} is not of ${KafkaEvents.EventMediaParameterExtractCreated}, swapping to found event" }
            } else {
                log.info { "${event.referenceId} ${event.event} is not of ${KafkaEvents.EventMediaParameterExtractCreated}, could not find required event.." }
            }
            sevent ?: event
        } else event

        val batchEvents = createMessagesByArgs(forwardEvent)

        batchEvents.forEach { e ->
            val createdTask = if (e is FfmpegWorkRequestCreated) {
                FfmpegTaskData(
                    inputFile = e.inputFile,
                    outFile = e.outFile,
                    arguments = e.arguments
                ).let { task ->
                    val status = taskManager.createTask(referenceId = event.referenceId, eventId = UUID.randomUUID().toString(), derivedFromEventId = event.eventId, task= TaskType.Extract, data = Gson().toJson(task))
                    if (!status) {
                        log.error { "Failed to create Extract task on ${forwardEvent.referenceId}@${forwardEvent.eventId}" }
                    }
                    status
                }
            } else false
            if (createdTask)
                onResult(e)
        }
        return null
    }
}