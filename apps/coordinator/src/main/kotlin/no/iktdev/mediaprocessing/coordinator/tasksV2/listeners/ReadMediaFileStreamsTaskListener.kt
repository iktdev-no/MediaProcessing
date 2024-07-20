package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.data.dataAs
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.runner.CodeToOutput
import no.iktdev.mediaprocessing.shared.common.runner.getOutputUsing
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.EventsListenerContract
import no.iktdev.mediaprocessing.shared.contract.EventsManagerContract
import no.iktdev.mediaprocessing.shared.contract.data.Event
import no.iktdev.mediaprocessing.shared.contract.data.MediaFileStreamsReadEvent
import no.iktdev.mediaprocessing.shared.contract.data.StartEventData
import no.iktdev.mediaprocessing.shared.contract.dto.StartOperationEvents
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class ReadMediaFileStreamsTaskListener() : CoordinatorEventListener() {

    override fun getProducerName(): String {
        return this::class.java.simpleName
    }

    @Autowired
    override var coordinator: Coordinator? = null

    val log = KotlinLogging.logger {}
    val requiredOperations = listOf(StartOperationEvents.ENCODE, StartOperationEvents.EXTRACT)

    override val produceEvent: Events = Events.EventMediaReadStreamPerformed
    override val listensForEvents: List<Events> = listOf(Events.EventMediaProcessStarted)

    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        val status =  super.shouldIProcessAndHandleEvent(incomingEvent, events)
        return status
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume()
        if (event == null) {
            log.error { "Event is null and should not be available! ${WGson.gson.toJson(incomingEvent.metadata())}" }
            return
        }

        val startEvent = event.dataAs<StartEventData>()
        if (startEvent == null || !startEvent.operations.any { it in requiredOperations }) {
            log.info { "${event.metadata.referenceId} does not contain a operation in ${requiredOperations.joinToString(",") { it.name }}" }
            return
        }
        val result = runBlocking {
            try {
                val data = fileReadStreams(startEvent, event.metadata.eventId)
                MediaFileStreamsReadEvent(
                    metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
                    data = data
                )
            } catch (e: Exception) {
                e.printStackTrace()
                MediaFileStreamsReadEvent(
                    metadata = event.makeDerivedEventInfo(EventStatus.Failed, getProducerName())
                )
            }
        }
        onProduceEvent(result)
    }


    suspend fun fileReadStreams(started: StartEventData, eventId: String): JsonObject? {
        val file = File(started.file)
        return if (file.exists() && file.isFile) {
            val result = readStreams(file)
            val joined = result.output.joinToString(" ")
            Gson().fromJson(joined, JsonObject::class.java)
        } else {
            val message = "File in data is not a file or does not exist".also {
                log.error { it }
            }
            throw RuntimeException(message)

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