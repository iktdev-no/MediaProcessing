package no.iktdev.mediaprocessing.shared.kafka.core

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.work.ProcesserEncodeWorkPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.work.ProcesserExtractWorkPerformed

class DeserializingRegistry {
    private val log = KotlinLogging.logger {}

    companion object {
        val deserializables = mutableMapOf(
            KafkaEvents.EventMediaProcessStarted to MediaProcessStarted::class.java,
            KafkaEvents.EventMediaReadStreamPerformed to ReaderPerformed::class.java,
            KafkaEvents.EventMediaParseStreamPerformed to MediaStreamsParsePerformed::class.java,
            KafkaEvents.EventMediaReadBaseInfoPerformed to BaseInfoPerformed::class.java,
            KafkaEvents.EventMediaMetadataSearchPerformed to MetadataPerformed::class.java,
            KafkaEvents.EventMediaReadOutNameAndType to VideoInfoPerformed::class.java,
            KafkaEvents.EventMediaReadOutCover to CoverInfoPerformed::class.java,
            KafkaEvents.EventMediaParameterEncodeCreated to FfmpegWorkerArgumentsCreated::class.java,
            KafkaEvents.EventMediaParameterExtractCreated to FfmpegWorkerArgumentsCreated::class.java,
            KafkaEvents.EventMediaParameterConvertCreated to null,
            KafkaEvents.EventMediaParameterDownloadCoverCreated to null,

            KafkaEvents.EventNotificationOfWorkItemRemoval to NotificationOfDeletionPerformed::class.java,

            KafkaEvents.EventWorkEncodeCreated to FfmpegWorkRequestCreated::class.java,
            KafkaEvents.EventWorkExtractCreated to FfmpegWorkRequestCreated::class.java,
            KafkaEvents.EventWorkConvertCreated to ConvertWorkerRequest::class.java,

            KafkaEvents.EventWorkEncodePerformed to ProcesserEncodeWorkPerformed::class.java,
            KafkaEvents.EventWorkExtractPerformed to ProcesserExtractWorkPerformed::class.java,
            KafkaEvents.EventWorkConvertPerformed to ConvertWorkPerformed::class.java,
            KafkaEvents.EventWorkDownloadCoverPerformed to CoverDownloadWorkPerformed::class.java,


            KafkaEvents.EVENT_MEDIA_PROCESS_COMPLETED to ProcessCompleted::class.java
            )
    }

    fun deserialize(event: KafkaEvents, json: String): Message<out MessageDataWrapper> {
        val gson = Gson()
        val dezClazz = deserializables[event]
        if (dezClazz == null) {
            log.warn { "${event.event} will be deserialized with default!" }
        }
        dezClazz?.let { eventClass ->
            try {
                val type = TypeToken.getParameterized(Message::class.java, eventClass).type
                return gson.fromJson<Message<MessageDataWrapper>>(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Fallback
        val type = object : TypeToken<Message<out SimpleMessageData>>() {}.type
        return gson.fromJson<Message<SimpleMessageData>>(json, type)
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