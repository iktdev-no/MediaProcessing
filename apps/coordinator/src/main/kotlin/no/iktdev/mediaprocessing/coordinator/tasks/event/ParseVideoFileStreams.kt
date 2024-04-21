package no.iktdev.mediaprocessing.coordinator.tasks.event

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.lastOrSuccessOf
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.AudioStream
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.SubtitleStream
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.VideoStream
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaStreamsParsePerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ReaderPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ParseVideoFileStreams(@Autowired override var coordinator: Coordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}


    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventMediaParseStreamPerformed

    override val requiredEvents: List<KafkaEvents> = listOf(
        KafkaEvents.EventMediaReadStreamPerformed
    )

    override fun prerequisitesRequired(events: List<PersistentMessage>): List<() -> Boolean> {
        return super.prerequisitesRequired(events) + listOf {
            isPrerequisiteDataPresent(events)
        }
    }

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        super.onProcessEvents(event, events)

        log.info { "${event.referenceId} triggered by ${event.event}" }
        val desiredEvent = events.lastOrSuccessOf(KafkaEvents.EventMediaReadStreamPerformed) ?: return null
        return parseStreams(desiredEvent.data as ReaderPerformed, desiredEvent.eventId)
    }

    fun parseStreams(data: ReaderPerformed, eventId: String): MessageDataWrapper {
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
            MediaStreamsParsePerformed(Status.COMPLETED, parsedStreams, eventId)

        } catch (e: Exception) {
            e.printStackTrace()
            SimpleMessageData(Status.ERROR, message = e.message, eventId)
        }

    }

}