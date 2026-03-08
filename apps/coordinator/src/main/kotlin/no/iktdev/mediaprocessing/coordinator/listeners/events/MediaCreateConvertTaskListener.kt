package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.events.SoftDispatchException
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ConvertTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.isOnly
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
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

    private val allowedExtensions = setOf("smi", "srt", "ass", "vtt")

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {

        val startedEvent = history.filterIsInstance<StartProcessingEvent>().firstOrNull() ?: return null
        if (startedEvent.data.operation.isNotEmpty()) {
            if (!startedEvent.data.operation.contains(OperationType.ConvertSubtitles))
                return null
        }

        val convertTask = if (startedEvent.data.operation.isOnly(OperationType.ConvertSubtitles)) {
            createTaskFromDirect(startedEvent)
        } else {
            try {
                createTaskFromNormalFlow(event)
            } catch (e: Exception) {
                return null
            }
        }

        val taskCreatedEvent = ConvertTaskCreatedEvent(convertTask.taskId).derivedOf(event)

        TaskStore.persist(convertTask.apply { derivedOf(taskCreatedEvent) })

        return taskCreatedEvent
    }

    fun createTaskFromNormalFlow(event: Event): ConvertTask {
        val extractEvent = event.requireQualifiedEntry<ProcesserExtractResultEvent>()
        if (extractEvent.status != TaskStatus.Completed) {
            throw SoftDispatchException.ForcedListenerEjectionException("Inserted extract event is not successful!", event::class.java)
        }

        val result = extractEvent.data ?: run {
            throw SoftDispatchException.ForcedListenerEjectionException("Extract event is missing data required to proceed", event::class.java)
        }
        if (!Files.exists(Path.of(result.cachedOutputFile))) {
            throw SoftDispatchException.ForcedListenerEjectionException("Extract event's output file is missing", event::class.java)
        }
        val useFile = File(result.cachedOutputFile)

        return ConvertTask(
            data = ConvertTask.Data(
                inputFile = result.cachedOutputFile,
                language = result.language,
                allowOverwrite = allowOverwrite(),
                outputDirectory = useFile.parentFile.absolutePath,
                outputFileName = useFile.nameWithoutExtension,
            )
        )
    }

    fun createTaskFromDirect(startEvent: StartProcessingEvent): ConvertTask {
        validateInputExtension(startEvent)
        val sourceFile = startEvent.data.fileUri.let { File(it) }
        val language = sourceFile.parentFile.nameWithoutExtension // We always expect the parent file to be eks "eng", might be smart to validate that name is max 3
        val outputDirectory = sourceFile.parentFile.absolutePath
        val outputFileName = sourceFile.nameWithoutExtension

        return ConvertTask(
            data = ConvertTask.Data(
                inputFile = sourceFile.absolutePath,
                language = language,
                allowOverwrite = allowOverwrite(),
                outputDirectory = outputDirectory,
                outputFileName = outputFileName,
            )
        )
    }

    fun validateInputExtension(startEvent: StartProcessingEvent) {
        val sourceFile = startEvent.data.fileUri.let { File(it) }
        val ext = sourceFile.extension.lowercase()

        if (ext !in allowedExtensions) {
            throw SoftDispatchException.ForcedListenerEjectionException(
                "Input file extension '$ext' is not supported. Allowed: $allowedExtensions",
                startEvent::class.java
            )
        }
    }


}