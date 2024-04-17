package no.iktdev.mediaprocessing.shared.kafka.core

enum class KafkaEvents(val event: String) {
    EventMediaProcessStarted("event:media-process:started"),
    EVENT_REQUEST_PROCESS_STARTED("event:request-process:started"),

    EventMediaReadStreamPerformed("event:media-read-stream:performed"),
    EventMediaParseStreamPerformed("event:media-parse-stream:performed"),
    EventMediaReadBaseInfoPerformed("event:media-read-base-info:performed"),
    EventMediaMetadataSearchPerformed("event:media-metadata-search:performed"),
    EventMediaReadOutNameAndType("event:media-read-out-name-and-type:performed"),
    EventMediaReadOutCover("event:media-read-out-cover:performed"),

    EventMediaParameterEncodeCreated("event:media-encode-parameter:created"),
    EventMediaParameterExtractCreated("event:media-extract-parameter:created"),
    EventMediaParameterConvertCreated("event:media-convert-parameter:created"),
    EventMediaParameterDownloadCoverCreated("event:media-download-cover-parameter:created"),

    EventMediaWorkProceedPermitted("event:media-work-proceed:permitted"),

    // This event is to be used for commuincating across all appss taht an event has ben removed and to rterminate existint events
    EventNotificationOfWorkItemRemoval("event:notification-work-item-removal"),

    EventWorkEncodeCreated("event:work-encode:created"),
    EventWorkExtractCreated("event:work-extract:created"),
    EventWorkConvertCreated("event:work-convert:created"),

    EventWorkEncodePerformed("event:work-encode:performed"),
    EventWorkExtractPerformed("event:work-extract:performed"),
    EventWorkConvertPerformed("event:work-convert:performed"),
    EventWorkDownloadCoverPerformed("event:work-download-cover:performed"),


    EVENT_STORE_VIDEO_PERFORMED("event:store-video:performed"),
    EVENT_STORE_SUBTITLE_PERFORMED("event:store-subtitle:performed"),
    EVENT_STORE_COVER_PERFORMED("event:store-cover:performed"),
    EVENT_STORE_METADATA_PERFORMED("event:store-metadata:performed"),

    EventMediaProcessCompleted("event:media-process:completed"),
    EventRequestProcessCompleted("event:request-process:completed"),
    EventCollectAndStore("event::save"),

    ;

    companion object {
        fun toEvent(event: String): KafkaEvents? {
            return KafkaEvents.entries.find { it.event == event }
        }

        fun isOfWork(event: KafkaEvents): Boolean {
            return event in listOf(

                EventWorkConvertCreated,
                EventWorkExtractCreated,
                EventWorkEncodeCreated,

                EventWorkEncodePerformed,
                EventWorkConvertPerformed,
                EventWorkExtractPerformed
            )
        }

        fun isOfFinalize(event: KafkaEvents): Boolean {
            return event in listOf(
                EventMediaProcessCompleted,
                EventRequestProcessCompleted,
                EventCollectAndStore
            )
        }
    }
}