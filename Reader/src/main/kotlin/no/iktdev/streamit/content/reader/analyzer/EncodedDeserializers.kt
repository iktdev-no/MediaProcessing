package no.iktdev.streamit.content.reader.analyzer

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sun.net.httpserver.Authenticator.Success
import no.iktdev.streamit.content.common.streams.AudioStream
import no.iktdev.streamit.content.common.streams.MediaStreams
import no.iktdev.streamit.content.common.streams.SubtitleStream
import no.iktdev.streamit.content.common.streams.VideoStream
import no.iktdev.streamit.content.reader.fileWatcher.FileWatcher
import no.iktdev.streamit.library.kafka.KnownEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.sequential.IMessageDataDeserialization

class EncodedDeserializers {
    val gson = Gson()

    val fileReceived = object : IMessageDataDeserialization<FileWatcher.FileResult> {
        override fun deserialize(incomingMessage: Message): FileWatcher.FileResult? {
            if (incomingMessage.status.statusType != StatusType.SUCCESS) {
                return null
            }
            return incomingMessage.dataAs(FileWatcher.FileResult::class.java)
        }
    }

    val mediaStreams = object : IMessageDataDeserialization<MediaStreams> {
        override fun deserialize(incomingMessage: Message): MediaStreams? {
            return try {
                if (incomingMessage.status.statusType != StatusType.SUCCESS) {
                    return null
                }
                /*return gson.fromJson(streams.value().data as String, MediaStreams::class.java)*/
                val jsonObject = gson.fromJson(incomingMessage.dataAsJson(), JsonObject::class.java)

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
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    }

    fun getDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return mutableMapOf(
            KnownEvents.EVENT_READER_RECEIVED_FILE.event to fileReceived,
            KnownEvents.EVENT_READER_RECEIVED_STREAMS.event to mediaStreams
        )
    }

}