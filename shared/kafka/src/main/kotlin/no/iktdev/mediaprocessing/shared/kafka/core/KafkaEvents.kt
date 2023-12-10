package no.iktdev.mediaprocessing.shared.kafka.core

enum class KafkaEvents(val event: String) {
    EVENT_PROCESS_STARTED("event:process:started"),

    EVENT_MEDIA_READ_STREAM_PERFORMED("event:media-read-stream:performed"),
    EVENT_MEDIA_PARSE_STREAM_PERFORMED("event:media-parse-stream:performed"),
    EVENT_MEDIA_READ_BASE_INFO_PERFORMED("event:media-read-base-info:performed"),
    EVENT_MEDIA_METADATA_SEARCH_PERFORMED("event:media-metadata-search:performed"),
    EVENT_MEDIA_READ_OUT_NAME_AND_TYPE("event:media-read-out-name-and-type:performed"),

    EVENT_MEDIA_ENCODE_PARAMETER_CREATED("event:media-encode-parameter:created"),
    EVENT_MEDIA_EXTRACT_PARAMETER_CREATED("event:media-extract-parameter:created"),
    EVENT_MEDIA_CONVERT_PARAMETER_CREATED("event:media-convert-parameter:created"),
    EVENT_MEDIA_DOWNLOAD_COVER_PARAMETER_CREATED("event:media-download-cover-parameter:created"),

    EVENT_WORK_ENCODE_CREATED("event:work-encode:created"),
    EVENT_WORK_EXTRACT_CREATED("event:work-extract:created"),
    EVENT_WORK_CONVERT_CREATED("event:work-convert:created"),

    EVENT_WORK_ENCODE_PERFORMED("event:work-encode:performed"),
    EVENT_WORK_EXTRACT_PERFORMED("event:work-extract:performed"),
    EVENT_WORK_CONVERT_PERFORMED("event:work-convert:performed"),
    EVENT_WORK_DOWNLOAD_COVER_PERFORMED("event:work-download-cover:performed"),


    EVENT_WORK_ENCODE_SKIPPED("event:work-encode:skipped"),
    EVENT_WORK_EXTRACT_SKIPPED("event:work-extract:skipped"),
    EVENT_WORK_CONVERT_SKIPPED("event:work-convert:skipped"),


    EVENT_STORE_VIDEO_PERFORMED("event:store-video:performed"),
    EVENT_STORE_SUBTITLE_PERFORMED("event:store-subtitle:performed"),
    EVENT_STORE_COVER_PERFORMED("event:store-cover:performed"),
    EVENT_STORE_METADATA_PERFORMED("event:store-metadata:performed"),

    EVENT_PROCESS_COMPLETED("event:process:completed");

    companion object {
        fun toEvent(event: String): KafkaEvents? {
            return KafkaEvents.entries.find { it.event == event }
        }
    }
}