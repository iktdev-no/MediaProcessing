package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.data.dataAs
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.runner.CodeToOutput
import no.iktdev.mediaprocessing.shared.common.runner.getOutputUsing
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.dto.OperationEvents
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
    val requiredOperations = listOf(OperationEvents.ENCODE, OperationEvents.EXTRACT)

    override val produceEvent: Events = Events.StreamRead
    override val listensForEvents: List<Events> = listOf(Events.ProcessStarted)

    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        val status =  super.shouldIProcessAndHandleEvent(incomingEvent, events)
        val permittedOperations = events.findFirstEventOf<MediaProcessStartEvent>()?.data?.operations ?: return false
        return if (permittedOperations.any { it in requiredOperations }) {
            status
        } else {
            false
        }
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume()
        if (event == null) {
            log.error { "Event is null and should not be available! ${WGson.gson.toJson(incomingEvent.metadata())}" }
            return
        }
        active = true

        val startEvent = event.dataAs<StartEventData>()
        if (startEvent == null || !startEvent.operations.any { it in requiredOperations }) {
            log.info { "${event.metadata.referenceId} does not contain a operation in ${requiredOperations.joinToString(",") { it.name }}" }
            active = false
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
        active = false
    }

    override fun produceFailure(incomingEvent: Event) {
        onProduceEvent(
            MediaFileStreamsReadEvent(
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed, getProducerName()),
                data = null
            )
        )
    }


    suspend fun fileReadStreams(started: StartEventData, eventId: String): JsonObject? {
        val file = File(started.file)
        return if (file.exists() && file.isFile) {
            val result = readStreams(file)
            val joined = result.output.joinToString(" ")
            Gson().fromJson(joined, JsonObject::class.java)
        } else {
            val message = "File in data is not a file or does not exist: ${file.absolutePath}".also {
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