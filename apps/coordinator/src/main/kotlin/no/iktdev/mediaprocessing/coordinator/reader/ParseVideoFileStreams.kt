package no.iktdev.mediaprocessing.coordinator.reader

import com.google.gson.Gson
import kotlinx.coroutines.launch
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreatorListener
import no.iktdev.mediaprocessing.shared.common.ProcessingService
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.AudioStream
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.SubtitleStream
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.VideoStream
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaStreamsParsePerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ReaderPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ParseVideoFileStreams(@Autowired var coordinator: Coordinator): ProcessingService() {

    override fun onResult(referenceId: String, data: MessageDataWrapper) {
        producer.sendMessage(referenceId, KafkaEvents.EVENT_MEDIA_PARSE_STREAM_PERFORMED, data)
    }

    override fun onReady() {
        coordinator.addListener(object : TaskCreatorListener {
            override fun onEventReceived(referenceId: String, event: PersistentMessage, events: List<PersistentMessage>) {
                if (event.event == KafkaEvents.EVENT_MEDIA_READ_STREAM_PERFORMED && event.data.isSuccess()) {
                    io.launch {
                        val result = parseStreams(event.data as ReaderPerformed)
                        onResult(referenceId, result)
                    }
                }
            }

        })
    }


    fun parseStreams(data: ReaderPerformed): MessageDataWrapper {
        val gson = Gson()
        return try {
            val jStreams = data.output.getAsJsonArray("streams")

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
            MediaStreamsParsePerformed(Status.COMPLETED, parsedStreams)

        } catch (e: Exception) {
            e.printStackTrace()
            MessageDataWrapper(Status.ERROR, message = e.message)
        }

    }

}