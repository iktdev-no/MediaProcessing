package no.iktdev.mediaprocessing.coordinator.tasks.event

import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.lastOrSuccessOf
import no.iktdev.mediaprocessing.shared.common.parsing.FileNameParser
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class BaseInfoFromFile(@Autowired override var coordinator: EventCoordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}

    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventMediaReadBaseInfoPerformed

    override val requiredEvents: List<KafkaEvents> = listOf(KafkaEvents.EventMediaProcessStarted)


    override fun prerequisitesRequired(events: List<PersistentMessage>): List<() -> Boolean> {
        return super.prerequisitesRequired(events) + listOf {
            isPrerequisiteDataPresent(events)
        }
    }

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        super.onProcessEventsAccepted(event, events)
        log.info { "${event.referenceId} triggered by ${event.event}" }
        val selected = events.lastOrSuccessOf(KafkaEvents.EventMediaProcessStarted) ?: return null
        return readFileInfo(selected.data as MediaProcessStarted, event.eventId)
    }

    fun readFileInfo(started: MediaProcessStarted, eventId: String): MessageDataWrapper {
        val result = try {
            val fileName = File(started.file).nameWithoutExtension
            val fileNameParser = FileNameParser(fileName)
            BaseInfoPerformed(
                Status.COMPLETED,
                title = fileNameParser.guessDesiredTitle(),
                sanitizedName = fileNameParser.guessDesiredFileName(),
                searchTitles = fileNameParser.guessSearchableTitle(),
                derivedFromEventId = eventId
            )
        } catch (e: Exception) {
            e.printStackTrace()
            SimpleMessageData(Status.ERROR, e.message ?: "Unable to obtain proper info from file", eventId)
        }
        return result
    }


}