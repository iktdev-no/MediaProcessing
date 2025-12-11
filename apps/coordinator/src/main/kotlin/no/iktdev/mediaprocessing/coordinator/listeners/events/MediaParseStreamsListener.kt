package no.iktdev.mediaprocessing.coordinator.listeners.events

import com.google.gson.Gson
import com.google.gson.JsonObject
import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamReadEvent
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Order(4)
@Component
class MediaParseStreamsListener: EventListener() {
    val log = KotlinLogging.logger {}

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        if (event !is MediaStreamReadEvent) return null

        val streams = parseStreams(event.data)
        return MediaStreamParsedEvent(
            data = streams
        ).derivedOf(event)
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