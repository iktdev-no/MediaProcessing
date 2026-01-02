package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ConvertTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import no.iktdev.mediaprocessing.shared.common.stores.TaskStore
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@Component
class MediaCreateConvertTaskListener: EventListener() {
    private val log = KotlinLogging.logger {}

    fun allowOverwrite(): Boolean {
        return true
    }

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {

        val startedEvent = history.filterIsInstance<StartProcessingEvent>().firstOrNull() ?: return null
        if (startedEvent.data.operation.isNotEmpty()) {
            if (!startedEvent.data.operation.contains(OperationType.Convert))
                return null
        }
        val selectedEvent = event as? ProcesserExtractResultEvent ?: return null
        if (selectedEvent.status != TaskStatus.Completed)
            return null


        val result = selectedEvent.data ?: return null
        if (!Files.exists(Path.of(result.cachedOutputFile)))
            return null
        val useFile = File(result.cachedOutputFile)

        val convertTask = ConvertTask(
            data = ConvertTask.Data(
                inputFile = result.cachedOutputFile,
                language = result.language,
                allowOverwrite = allowOverwrite(),
                outputDirectory = useFile.parentFile.absolutePath,
                outputFileName = useFile.nameWithoutExtension,
            )
        ).derivedOf(event)
        TaskStore.persist(convertTask)

        return ConvertTaskCreatedEvent(convertTask.taskId)

    }
}