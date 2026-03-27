package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.FilePrepareForWorkTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ValidateFileAndMediaDataEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.FilePrepareForWorkTask
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileNotFoundException

@Component
class FilePrepareForWorkCreateTaskListener(
    private val env: CoordinatorEnv
): EventListener() {
    private val log = KotlinLogging.logger {}

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val validated = event.requireQualifiedEntry<ValidateFileAndMediaDataEvent>()
        if (validated.validationStatus != ValidateFileAndMediaDataEvent.ValidationStatus.Ok) {
            return null
        }
        if (validated.warnings.isNotEmpty()) {
            log.warn { "Validation warnings: ${validated.warnings}" }
        }


        val startedEvent = history.filterIsInstance<StartProcessingEvent>().firstOrNull() ?: return null
        if (startedEvent.data.operation.isNotEmpty()) {
            if (startedEvent.data.operation.none { it in listOf(OperationType.Encode, OperationType.ExtractSubtitles) })
                return null
        }

        val source = File(startedEvent.data.fileUri).absoluteFile
        val destination = env.scratchFolder.using(source.name)

        if (!source.exists()) {
            throw FileNotFoundException("File $source does not exist")
        }

        val task = FilePrepareForWorkTask(data = FilePrepareForWorkTask.Data(
            sourceFile = source.absolutePath,
            destinationFile = destination.absolutePath
        ))

        val createdTaskEvent = FilePrepareForWorkTaskCreatedEvent(taskId = task.taskId).derivedOf(event)
        task.apply { derivedOf(createdTaskEvent) }
        TaskStore.persist(task)
        return createdTaskEvent
    }
}