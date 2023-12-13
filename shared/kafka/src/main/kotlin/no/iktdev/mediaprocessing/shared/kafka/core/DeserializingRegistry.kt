package no.iktdev.mediaprocessing.shared.kafka.core

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import java.lang.reflect.Type
import kotlin.reflect.KClass

class DeserializingRegistry {
    companion object {
        val deserializables = mutableMapOf(
            KafkaEvents.EVENT_PROCESS_STARTED to ProcessStarted::class.java,
            KafkaEvents.EVENT_MEDIA_READ_STREAM_PERFORMED to ReaderPerformed::class.java,
            KafkaEvents.EVENT_MEDIA_PARSE_STREAM_PERFORMED to MediaStreamsParsePerformed::class.java,
            KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED to BaseInfoPerformed::class.java,
            KafkaEvents.EVENT_MEDIA_METADATA_SEARCH_PERFORMED to MetadataPerformed::class.java,
            KafkaEvents.EVENT_MEDIA_READ_OUT_NAME_AND_TYPE to null,
            KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED to null,
            KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED to null,
            KafkaEvents.EVENT_MEDIA_CONVERT_PARAMETER_CREATED to null,
            KafkaEvents.EVENT_MEDIA_DOWNLOAD_COVER_PARAMETER_CREATED to null,

            KafkaEvents.EVENT_WORK_ENCODE_CREATED to null,
            KafkaEvents.EVENT_WORK_EXTRACT_CREATED to null,
            KafkaEvents.EVENT_WORK_CONVERT_CREATED to null,

            KafkaEvents.EVENT_WORK_ENCODE_PERFORMED to null,
            KafkaEvents.EVENT_WORK_EXTRACT_PERFORMED to null,
            KafkaEvents.EVENT_WORK_CONVERT_PERFORMED to null,
            KafkaEvents.EVENT_WORK_DOWNLOAD_COVER_PERFORMED to null,

            KafkaEvents.EVENT_WORK_ENCODE_SKIPPED to null,
            KafkaEvents.EVENT_WORK_EXTRACT_SKIPPED to null,
            KafkaEvents.EVENT_WORK_CONVERT_SKIPPED to null,
            )
    }

    fun deserialize(event: KafkaEvents, json: String): Message<out MessageDataWrapper> {
        val gson = Gson()
        val dezClazz = deserializables[event]
        dezClazz?.let { eventClass ->
            try {
                val type = TypeToken.getParameterized(Message::class.java, eventClass).type
                return gson.fromJson<Message<MessageDataWrapper>>(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Fallback
        val type = object : TypeToken<Message<out MessageDataWrapper>>() {}.type
        return gson.fromJson<Message<MessageDataWrapper>>(json, type)
    }

    fun deserializeData(event: KafkaEvents, json: String): MessageDataWrapper {
        val gson = Gson()
        val dezClazz = deserializables[event]
        dezClazz?.let { eventClass ->
            try {
                val type = TypeToken.getParameterized(eventClass).type
                return gson.fromJson<MessageDataWrapper>(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        try {
            // Fallback
            val type = object : TypeToken<SimpleMessageData>() {}.type
            return gson.fromJson<SimpleMessageData>(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Default
        val type = object : TypeToken<MessageDataWrapper>() {}.type
        return gson.fromJson<MessageDataWrapper>(json, type)
    }

}