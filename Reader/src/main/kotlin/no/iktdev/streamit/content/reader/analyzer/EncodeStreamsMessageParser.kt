package no.iktdev.streamit.content.reader.analyzer

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import no.iktdev.streamit.content.common.streams.*
import no.iktdev.streamit.content.reader.fileWatcher.FileWatcher
import no.iktdev.streamit.library.kafka.KnownEvents
import no.iktdev.streamit.library.kafka.Message
import no.iktdev.streamit.library.kafka.StatusType
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.io.File

class EncodeStreamsMessageParser {
    fun getFileNameFromEvent(records: MutableList<ConsumerRecord<String, Message>>): FileWatcher.FileResult? {
        val file = records.find { it.key() == KnownEvents.EVENT_READER_RECEIVED_FILE.event } ?: return null
        if (file.value().status.statusType != StatusType.SUCCESS) return null
        return if (file.value().data is String) {
            return Gson().fromJson(file.value().data as String, FileWatcher.FileResult::class.java)
        } else null
    }

    fun getMediaStreamsFromEvent(records: MutableList<ConsumerRecord<String, Message>>): MediaStreams? {
        val streams = records.find { it.key() == KnownEvents.EVENT_READER_RECEIVED_STREAMS.event } ?: return null
        if (streams.value().status.statusType != StatusType.SUCCESS || streams.value().data !is String) return null
        val json = streams.value().data as String
        val gson = Gson()
        /*return gson.fromJson(streams.value().data as String, MediaStreams::class.java)*/

        val jsonObject = gson.fromJson(json, JsonObject::class.java)

        val streamsJsonArray = jsonObject.getAsJsonArray("streams")

        val rstreams = streamsJsonArray.mapNotNull { streamJson ->
            val streamObject = streamJson.asJsonObject

            val codecType = streamObject.get("codec_type").asString
            if (streamObject.has("codec_name") && streamObject.get("codec_name").asString == "mjpeg") {
                null
            } else {
                when (codecType) {
                    "video" -> gson.fromJson(streamObject, VideoStream::class.java)
                    "audio" -> gson.fromJson(streamObject, AudioStream::class.java)
                    "subtitle" -> gson.fromJson(streamObject, SubtitleStream::class.java)
                    else -> null //throw IllegalArgumentException("Unknown stream type: $codecType")
                }
            }
        }

        return MediaStreams(rstreams)
    }

}