package no.iktdev.mediaprocessing.shared.contract

enum class Events(val event: String) {
    EventMediaProcessStarted                ("event:media-process:started"),

    EventMediaReadStreamPerformed           ("event:media-read-stream:performed"),
    EventMediaParseStreamPerformed          ("event:media-parse-stream:performed"),
    EventMediaReadBaseInfoPerformed         ("event:media-read-base-info:performed"),
    EventMediaMetadataSearchPerformed       ("event:media-metadata-search:performed"),
    EventMediaReadOutNameAndType            ("event:media-read-out-name-and-type:performed"),
    EventMediaReadOutCover                  ("event:media-read-out-cover:performed"),

    EventMediaParameterEncodeCreated        ("event:media-encode-parameter:created"),
    EventMediaParameterExtractCreated       ("event:media-extract-parameter:created"),
    EventMediaParameterDownloadCoverCreated ("event:media-download-cover-parameter:created"),

    EventMediaWorkProceedPermitted          ("event:media-work-proceed:permitted"),

    EventNotificationOfWorkItemRemoval("event:notification-work-item-removal"),

    EventWorkEncodeCreated                  ("event:work-encode:created"),
    EventWorkExtractCreated                 ("event:work-extract:created"),
    EventWorkConvertCreated                 ("event:work-convert:created"),

    EventWorkEncodePerformed                ("event:work-encode:performed"),
    EventWorkExtractPerformed               ("event:work-extract:performed"),
    EventWorkConvertPerformed               ("event:work-convert:performed"),
    EventWorkDownloadCoverPerformed         ("event:work-download-cover:performed"),

    EVENT_STORE_VIDEO_PERFORMED             ("event:store-video:performed"),
    EVENT_STORE_SUBTITLE_PERFORMED          ("event:store-subtitle:performed"),
    EVENT_STORE_COVER_PERFORMED             ("event:store-cover:performed"),
    EVENT_STORE_METADATA_PERFORMED          ("event:store-metadata:performed"),

    EventMediaProcessCompleted              ("event:media-process:completed"),
    EventCollectAndStore                    ("event::save"),

    ;

    companion object {
        fun toEvent(event: String): Events? {
            return Events.entries.find { it.event == event }
        }

        fun isOfWork(event: Events): Boolean {
            return event in listOf(

                EventWorkConvertCreated,
                EventWorkExtractCreated,
                EventWorkEncodeCreated,

                EventWorkEncodePerformed,
                EventWorkConvertPerformed,
                EventWorkExtractPerformed
            )
        }

        fun isOfFinalize(event: Events): Boolean {
            return event in listOf(
                EventMediaProcessCompleted,
                EventCollectAndStore
            )
        }
    }
}