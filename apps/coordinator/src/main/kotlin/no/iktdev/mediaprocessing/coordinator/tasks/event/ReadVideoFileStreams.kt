package no.iktdev.mediaprocessing.coordinator.tasks.event

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.runner.CodeToOutput
import no.iktdev.mediaprocessing.shared.common.runner.getOutputUsing
import no.iktdev.mediaprocessing.shared.contract.dto.StartOperationEvents
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ReaderPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class ReadVideoFileStreams(@Autowired override var coordinator: EventCoordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}
    val requiredOperations = listOf(StartOperationEvents.ENCODE, StartOperationEvents.EXTRACT)

    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventMediaReadStreamPerformed

    override val requiredEvents: List<KafkaEvents> = listOf(
        KafkaEvents.EventMediaProcessStarted
    )


    override fun prerequisitesRequired(events: List<PersistentMessage>): List<() -> Boolean> {
        return super.prerequisitesRequired(events) + listOf {
            isPrerequisiteDataPresent(events)
        }
    }


    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        super.onProcessEventsAccepted(event, events)
        log.info { "${event.referenceId} triggered by ${event.event}" }
        val desiredEvent = events.find { it.data is MediaProcessStarted } ?: return null
        val data = desiredEvent.data as MediaProcessStarted
        if (!data.operations.any { it in requiredOperations }) {
            log.info { "${event.referenceId} does not contain a operation in ${requiredOperations.joinToString(",") { it.name }}" }
            return null
        }
        return runBlocking { fileReadStreams(data, desiredEvent.eventId) }
    }

    suspend fun fileReadStreams(started: MediaProcessStarted, eventId: String): MessageDataWrapper {
        val file = File(started.file)
        return if (file.exists() && file.isFile) {
            val result = readStreams(file)
            val joined = result.output.joinToString(" ")
            val jsoned = Gson().fromJson(joined, JsonObject::class.java)
            ReaderPerformed(Status.COMPLETED, file = started.file, output = jsoned, derivedFromEventId = eventId)
        } else {
            SimpleMessageData(Status.ERROR, "File in data is not a file or does not exist", eventId)
        }
    }

    suspend fun readStreams(file: File): CodeToOutput {
        val result = getOutputUsing(
            SharedConfig.ffprobe,
            "-v", "quiet", "-print_format", "json", "-show_streams", file.absolutePath
        )
        return result
    }

}