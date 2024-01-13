package no.iktdev.mediaprocessing.coordinator.tasks.event

import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.parsing.FileNameParser
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.springframework.stereotype.Service
import java.io.File

@Service
class BaseInfoFromFile() : TaskCreator() {

    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED

    override val requiredEvents: List<KafkaEvents> = listOf(KafkaEvents.EVENT_PROCESS_STARTED)


    override fun prerequisitesRequired(events: List<PersistentMessage>): List<() -> Boolean> {
        return super.prerequisitesRequired(events) + listOf {
            isPrerequisiteDataPresent(events)
        }
    }

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper {
        log.info { "${this.javaClass.simpleName} triggered by ${event.event}" }
        return readFileInfo(event.data as ProcessStarted)
    }

    fun readFileInfo(started: ProcessStarted): MessageDataWrapper {
        val result = try {
            val fileName = File(started.file).nameWithoutExtension
            val fileNameParser = FileNameParser(fileName)
            BaseInfoPerformed(
                Status.COMPLETED,
                title = fileNameParser.guessDesiredTitle(),
                sanitizedName = fileNameParser.guessDesiredFileName()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            MessageDataWrapper(Status.ERROR, e.message ?: "Unable to obtain proper info from file")
        }
        return result
    }


}