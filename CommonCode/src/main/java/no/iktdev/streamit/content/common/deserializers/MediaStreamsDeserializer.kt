package no.iktdev.streamit.content.common.deserializers

import com.google.gson.Gson
import com.google.gson.JsonObject
import no.iktdev.streamit.content.common.streams.AudioStream
import no.iktdev.streamit.content.common.streams.MediaStreams
import no.iktdev.streamit.content.common.streams.SubtitleStream
import no.iktdev.streamit.content.common.streams.VideoStream
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization

class MediaStreamsDeserializer: IMessageDataDeserialization<MediaStreams> {
    override fun deserialize(incomingMessage: Message): MediaStreams? {
        return try {
            val gson = Gson()
            val jsonObject = if (incomingMessage.data is String) {
                gson.fromJson(incomingMessage.data as String, JsonObject::class.java)
            } else {
                gson.fromJson(incomingMessage.dataAsJson(), JsonObject::class.java)
            }

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