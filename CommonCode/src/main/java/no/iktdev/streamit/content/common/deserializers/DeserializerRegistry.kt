package no.iktdev.streamit.content.common.deserializers

import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization

class DeserializerRegistry {
    companion object {
        private val _registry = mutableMapOf<KafkaEvents, IMessageDataDeserialization<*>>(
            KafkaEvents.EVENT_READER_RECEIVED_FILE to FileResultDeserializer(),
            KafkaEvents.EVENT_READER_RECEIVED_STREAMS to MediaStreamsDeserializer(),
            KafkaEvents.EVENT_METADATA_OBTAINED to MetadataResultDeserializer(),
            KafkaEvents.EVENT_READER_DETERMINED_SERIE to EpisodeInfoDeserializer(),
            KafkaEvents.EVENT_READER_DETERMINED_MOVIE to MovieInfoDeserializer(),
            KafkaEvents.EVENT_READER_DETERMINED_FILENAME to ContentOutNameDeserializer(),

            KafkaEvents.EVENT_READER_ENCODE_GENERATED_VIDEO to EncodeWorkDeserializer(),
            KafkaEvents.EVENT_ENCODER_VIDEO_FILE_QUEUED to EncodeWorkDeserializer(),
            KafkaEvents.EVENT_ENCODER_VIDEO_FILE_STARTED to EncodeWorkDeserializer(),

            KafkaEvents.EVENT_ENCODER_VIDEO_FILE_ENDED to EncodeWorkDeserializer(),
            KafkaEvents.EVENT_READER_ENCODE_GENERATED_SUBTITLE to ExtractWorkDeserializer(),
            KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_ENDED to ExtractWorkDeserializer(),
            KafkaEvents.EVENT_CONVERTER_SUBTITLE_FILE_ENDED to ConvertWorkDeserializer()

        )
        fun getRegistry(): Map<KafkaEvents, IMessageDataDeserialization<*>> = _registry.toMap()
        fun getEventToDeserializer(vararg keys: KafkaEvents): Map<String, IMessageDataDeserialization<*>> {
            val missingFields = keys.filter { !getRegistry().keys.contains(it) }

            if (missingFields.isNotEmpty()) {
                throw MissingDeserializerException("Missing deserializers for: ${missingFields.joinToString(", ")}")
            }
            return getRegistry().filter { keys.contains(it.key) }.map { it.key.event to it.value }.toMap()
        }

        private fun toEvent(event: String): KafkaEvents? {
            return KafkaEvents.values().find { it.event == event }
        }

        fun getDeserializerForEvent(event: String): IMessageDataDeserialization<*>? {
            val deszEvent = toEvent(event) ?: return null
            return getEventToDeserializer(deszEvent).values.first()
        }

        fun addDeserializer(key: KafkaEvents, deserializer: IMessageDataDeserialization<*>) {
            _registry[key] = deserializer
        }

    }
}

class MissingDeserializerException(override val message: String): RuntimeException()