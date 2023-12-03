package no.iktdev.mediaprocessing.coordinator.reader

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.SharedConfig
import no.iktdev.mediaprocessing.shared.ffmpeg.AudioStream
import no.iktdev.mediaprocessing.shared.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.ffmpeg.SubtitleStream
import no.iktdev.mediaprocessing.shared.ffmpeg.VideoStream
import no.iktdev.mediaprocessing.shared.kafka.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ReaderPerformed
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.stereotype.Service


@Service
class ParseVideoFileStreams {
    val io = Coroutines.io()
    val listener = DefaultMessageListener(SharedConfig.kafkaTopic) { event ->
        val message = event.value()
        if (message.data is ReaderPerformed) {
            io.launch {
                parseStreams(message.referenceId, message.data as ReaderPerformed)
            }
        }
    }
    val producer = CoordinatorProducer()

    init {
        io.launch {
            listener.listen()
        }
    }

    suspend fun parseStreams(referenceId: String, data: ReaderPerformed) {
        val gson = Gson()
        try {
            val jsonObject = gson.fromJson(data.output, JsonObject::class.java)
            val jStreams = jsonObject.getAsJsonArray("streams")

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
            producer.sendMessage(referenceId, KafkaEvents.EVENT_MEDIA_PARSE_STREAM_PERFORMED,
                MessageDataWrapper(Status.COMPLETED, gson.toJson(parsedStreams)
                )
            )

        } catch (e: Exception) {
            e.printStackTrace()
            producer.sendMessage(referenceId, KafkaEvents.EVENT_MEDIA_PARSE_STREAM_PERFORMED,
                MessageDataWrapper(Status.ERROR, message = e.message)
            )
        }

    }

}