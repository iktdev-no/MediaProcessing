package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import com.google.gson.Gson
import com.google.gson.JsonObject
import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.data.dataAs
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.EventsListenerContract
import no.iktdev.mediaprocessing.shared.contract.EventsManagerContract
import no.iktdev.mediaprocessing.shared.contract.data.Event
import no.iktdev.mediaprocessing.shared.contract.data.MediaFileStreamsParsedEvent
import no.iktdev.mediaprocessing.shared.contract.data.MediaFileStreamsReadEvent
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.AudioStream
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.SubtitleStream
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.VideoStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ParseMediaFileStreamsTaskListener() : CoordinatorEventListener() {
    val log = KotlinLogging.logger {}

    @Autowired
    override var coordinator: Coordinator? = null


    override val produceEvent: Events = Events.EventMediaParseStreamPerformed
    override val listensForEvents: List<Events> = listOf(
        Events.EventMediaReadStreamPerformed
    )

    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        return super.shouldIProcessAndHandleEvent(incomingEvent, events)
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume()
        if (event == null) {
            log.error { "Event is null and should not be available! ${WGson.gson.toJson(incomingEvent.metadata())}" }
            return
        }

        val readData = event.dataAs<JsonObject>()
        val result = try {
            MediaFileStreamsParsedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Success),
                data = parseStreams(readData)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            MediaFileStreamsParsedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Failed)
            )
        }
        onProduceEvent(result)
    }


    fun parseStreams(data: JsonObject?): ParsedMediaStreams {
        val ignoreCodecs = listOf("png", "mjpeg")
        val gson = Gson()
        return try {
            val jStreams = data!!.getAsJsonArray("streams")

            val videoStreams = mutableListOf<VideoStream>()
            val audioStreams = mutableListOf<AudioStream>()
            val subtitleStreams = mutableListOf<SubtitleStream>()

            for (streamJson in jStreams) {
                val streamObject = streamJson.asJsonObject
                if (!streamObject.has("codec_name")) continue
                val codecName = streamObject.get("codec_name").asString
                val codecType = streamObject.get("codec_type").asString

                if (codecName in ignoreCodecs) continue

                when (codecType) {
                    "video" -> videoStreams.add(gson.fromJson(streamObject, VideoStream::class.java))
                    "audio" -> audioStreams.add(gson.fromJson(streamObject, AudioStream::class.java))
                    "subtitle" -> subtitleStreams.add(gson.fromJson(streamObject, SubtitleStream::class.java))
                }
            }

            val parsedStreams = ParsedMediaStreams(
                videoStream = videoStreams,
                audioStream = audioStreams,
                subtitleStream = subtitleStreams
            )
            parsedStreams

        } catch (e: Exception) {
            "Failed to parse data, its either not a valid json structure or expected and required fields are not present.".also {
                log.error { it }
            }
            throw e
        }

    }

}