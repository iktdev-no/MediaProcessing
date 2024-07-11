package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.data.EventStatus
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.shared.common.parsing.FileNameParser
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.data.BaseInfo
import no.iktdev.mediaprocessing.shared.contract.data.BaseInfoEvent
import no.iktdev.mediaprocessing.shared.contract.data.Event
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class BaseInfoFromFileTaskListener() : CoordinatorEventListener() {
    @Autowired
    override var coordinator: Coordinator? = null

    val log = KotlinLogging.logger {}

    override val produceEvent: Events = Events.EventMediaReadBaseInfoPerformed
    override val listensForEvents: List<Events> = listOf(Events.EventMediaProcessStarted)



    override fun onEventsReceived(incomingEvent: Event, events: List<Event>) {
        val message = try {
            readFileInfo(incomingEvent.data as MediaProcessStarted, incomingEvent.metadata.eventId)?.let {
                BaseInfoEvent(metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Success), data = it)
            } ?: BaseInfoEvent(metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed))
        } catch (e: Exception) {
            BaseInfoEvent(metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed))
        }
        onProduceEvent(message)
    }


    @Throws(Exception::class)
    fun readFileInfo(started: MediaProcessStarted, eventId: String): BaseInfo? {
        return try {
            val fileName = File(started.file).nameWithoutExtension
            val fileNameParser = FileNameParser(fileName)
            BaseInfo(
                title = fileNameParser.guessDesiredTitle(),
                sanitizedName = fileNameParser.guessDesiredFileName(),
                searchTitles = fileNameParser.guessSearchableTitle()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            log.error { "Failed to read info from file\neventId: $eventId" }
            throw e
        }
    }


}