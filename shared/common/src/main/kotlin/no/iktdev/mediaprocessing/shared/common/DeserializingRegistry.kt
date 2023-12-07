package no.iktdev.mediaprocessing.shared.common

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import kotlin.reflect.KClass

class DeserializingRegistry {
    companion object {
        val deserializables = mutableListOf<Pair<KafkaEvents, KClass<out MessageDataWrapper>?>>(
            KafkaEvents.EVENT_PROCESS_STARTED to ProcessStarted::class,
            KafkaEvents.EVENT_MEDIA_READ_STREAM_PERFORMED to ReaderPerformed::class,
            KafkaEvents.EVENT_MEDIA_PARSE_STREAM_PERFORMED to MediaStreamsParsePerformed::class,
            KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED to BaseInfoPerformed::class,
            KafkaEvents.EVENT_MEDIA_METADATA_SEARCH_PERFORMED to MetadataPerformed::class,
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
}