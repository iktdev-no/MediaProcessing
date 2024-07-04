package no.iktdev.mediaprocessing.coordinator.tasks.event

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.taskManager
import no.iktdev.mediaprocessing.coordinator.tasks.event.ffmpeg.CreateProcesserWorkTask
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.persistance.TasksManager
import no.iktdev.mediaprocessing.shared.common.persistance.isOfEvent
import no.iktdev.mediaprocessing.shared.common.persistance.isSuccess
import no.iktdev.mediaprocessing.shared.common.task.FfmpegTaskData
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.az
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkerArgumentsCreated
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreateEncodeWorkTask(@Autowired override var coordinator: EventCoordinator) : CreateProcesserWorkTask(coordinator) {
    val log = KotlinLogging.logger {}
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventWorkEncodeCreated

    override val requiredEvents: List<KafkaEvents>
        get() = listOf(KafkaEvents.EventMediaParameterEncodeCreated)

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        super.onProcessEventsAccepted(event, events)
        log.info { "${event.referenceId} triggered by ${event.event}" }



        if (events.lastOrNull { it.isOfEvent(KafkaEvents.EventMediaParameterEncodeCreated) }?.isSuccess() != true) {
            return null
        }

        if (!isPermittedToCreateTasks(events)) {
            log.warn { "Cannot continue until permitted event is present" }
        }


        val forwardEvent = if (event.event != KafkaEvents.EventMediaParameterEncodeCreated) {
            val sevent = events.findLast { it.event == KafkaEvents.EventMediaParameterEncodeCreated }
            if (sevent != null) {
                log.info { "${event.referenceId} ${event.event} is not of ${KafkaEvents.EventMediaParameterEncodeCreated}, swapping to found event" }
            } else {
                log.info { "${event.referenceId} ${event.event} is not of ${KafkaEvents.EventMediaParameterEncodeCreated}, could not find required event.." }
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
                    val status = taskManager.createTask(
                        referenceId = event.referenceId,
                        derivedFromEventId = event.eventId,
                        task = TaskType.Encode,
                        data = Gson().toJson(task))
                    if (!status) {
                        log.error { "Failed to create Encode task on ${forwardEvent.referenceId}@${forwardEvent.eventId}" }
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