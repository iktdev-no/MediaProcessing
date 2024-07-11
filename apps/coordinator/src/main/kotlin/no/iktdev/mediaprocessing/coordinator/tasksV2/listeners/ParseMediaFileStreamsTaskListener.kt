package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import com.google.gson.Gson
import com.google.gson.JsonObject
import mu.KotlinLogging
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


    override fun onEventsReceived(incomingEvent: Event, events: List<Event>) {
        // MediaFileStreamsReadEvent
        val readData = incomingEvent.dataAs<MediaFileStreamsReadEvent>()?.data
        val result = try {
            MediaFileStreamsParsedEvent(
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Success),
                data = parseStreams(readData)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            MediaFileStreamsParsedEvent(
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed)
            )
        }
    }


    fun parseStreams(data: JsonObject?): ParsedMediaStreams {
        val gson = Gson()
        return try {
            val jStreams = data!!.getAsJsonArray("streams")

            val videoStreams = mutableListOf<VideoStream>()
            val audioStreams = mutableListOf<AudioStream>()
            val subtitleStreams = mutableListOf<SubtitleStream>()

            jStreams.forEach { streamJson ->
                val streamObject = streamJson.asJsonObject

                val codecType = streamObject.get("codec_type").asString
                if (streamObject.has("codec_name") && streamObject.get("codec_name").asString == "mjpeg") {
                } else {
                    when (codecType) {
                        "video" -> videoStreams.add(gson.fromJson(streamObject, VideoStream::class.java))
                        "audio" -> audioStreams.add(gson.fromJson(streamObject, AudioStream::class.java))
                        "subtitle" -> subtitleStreams.add(gson.fromJson(streamObject, SubtitleStream::class.java))
                    }
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