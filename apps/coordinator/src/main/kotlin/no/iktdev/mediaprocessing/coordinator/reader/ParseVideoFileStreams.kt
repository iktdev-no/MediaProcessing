package no.iktdev.mediaprocessing.coordinator.reader

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import no.iktdev.mediaprocessing.shared.common.ProcessingService
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.kafka.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.AudioStream
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.SubtitleStream
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.VideoStream
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ReaderPerformed
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.stereotype.Service


class ParseVideoFileStreams(producer: CoordinatorProducer = CoordinatorProducer(), listener: DefaultMessageListener = DefaultMessageListener(
    SharedConfig.kafkaTopic)): ProcessingService(producer, listener) {

    override fun onResult(referenceId: String, data: MessageDataWrapper) {
        producer.sendMessage(referenceId, KafkaEvents.EVENT_MEDIA_PARSE_STREAM_PERFORMED, data)
    }

    init {
        listener.onMessageReceived = { event ->
            val message = event.value
            if (message.data is ReaderPerformed) {
                io.launch {
                    val result = parseStreams(message.data as ReaderPerformed)
                    onResult(message.referenceId, result)
                }
            }
        }
        io.launch {
            listener.listen()
        }
    }


    fun parseStreams(data: ReaderPerformed): MessageDataWrapper {
        val gson = Gson()
        return try {
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
            MessageDataWrapper(Status.COMPLETED, gson.toJson(parsedStreams))

        } catch (e: Exception) {
            e.printStackTrace()
            MessageDataWrapper(Status.ERROR, message = e.message)
        }

    }

}