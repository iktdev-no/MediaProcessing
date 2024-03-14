package no.iktdev.mediaprocessing.coordinator.tasks.event

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.runner.CodeToOutput
import no.iktdev.mediaprocessing.shared.common.runner.getOutputUsing
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ReaderPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class ReadVideoFileStreams(@Autowired override var coordinator: Coordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}


    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EVENT_MEDIA_READ_STREAM_PERFORMED

    override val requiredEvents: List<KafkaEvents> = listOf(
        KafkaEvents.EVENT_PROCESS_STARTED
    )


    override fun prerequisitesRequired(events: List<PersistentMessage>): List<() -> Boolean> {
        return super.prerequisitesRequired(events) + listOf {
            isPrerequisiteDataPresent(events)
        }
    }


    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        log.info { "${this.javaClass.simpleName} triggered by ${event.event}" }
        val desiredEvent = events.find { it.data is ProcessStarted } ?: return null
        return runBlocking { fileReadStreams(desiredEvent.data as ProcessStarted) }
    }

    suspend fun fileReadStreams(started: ProcessStarted): MessageDataWrapper {
        val file = File(started.file)
        return if (file.exists() && file.isFile) {
            val result = readStreams(file)
            val joined = result.output.joinToString(" ")
            val jsoned = Gson().fromJson(joined, JsonObject::class.java)
            ReaderPerformed(Status.COMPLETED, file = started.file, output = jsoned)
        } else {
            SimpleMessageData(Status.ERROR, "File in data is not a file or does not exist")
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