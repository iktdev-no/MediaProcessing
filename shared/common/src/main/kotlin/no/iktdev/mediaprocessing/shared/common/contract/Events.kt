package no.iktdev.mediaprocessing.shared.common.contract

import no.iktdev.mediaprocessing.shared.common.contract.data.*

enum class Events(val event: String, val dataClass: Class<out Event>) {
    ProcessStarted                ("event:media-process:started", dataClass = MediaProcessStartEvent::class.java),

    ReadStreamPerformed           ("event:media-read-stream:performed", MediaFileStreamsReadEvent::class.java),
    ParseStreamPerformed          ("event:media-parse-stream:performed", MediaFileStreamsParsedEvent::class.java),
    ReadBaseInfoPerformed         ("event:media-read-base-info:performed", BaseInfoEvent::class.java),
    MetadataSearchPerformed       ("event:media-metadata-search:performed", MediaMetadataReceivedEvent::class.java),
    ReadOutNameAndType            ("event:media-read-out-name-and-type:performed", MediaOutInformationConstructedEvent::class.java),
    ReadOutCover                  ("event:media-read-out-cover:performed", MediaCoverInfoReceivedEvent::class.java),

    ParameterEncodeCreated        ("event:media-encode-parameter:created", EncodeArgumentCreatedEvent::class.java),
    ParameterExtractCreated       ("event:media-extract-parameter:created", ExtractArgumentCreatedEvent::class.java),

    //EventMediaParameterDownloadCoverCreated ("event:media-download-cover-parameter:created"),

    WorkProceedPermitted          ("event:media-work-proceed:permitted", PermitWorkCreationEvent::class.java),

    //EventNotificationOfWorkItemRemoval("event:notification-work-item-removal"),

    WorkEncodeCreated                  ("event:work-encode:created", EncodeWorkCreatedEvent::class.java),
    WorkExtractCreated                 ("event:work-extract:created", ExtractWorkCreatedEvent::class.java),
    WorkConvertCreated                 ("event:work-convert:created", ConvertWorkCreatedEvent::class.java),

    WorkEncodePerformed                ("event:work-encode:performed", EncodeWorkPerformedEvent::class.java),
    WorkExtractPerformed               ("event:work-extract:performed", ExtractWorkPerformedEvent::class.java),
    WorkConvertPerformed               ("event:work-convert:performed", ConvertWorkPerformed::class.java),
    WorkDownloadCoverPerformed         ("event:work-download-cover:performed", MediaCoverDownloadedEvent::class.java),

    PersistContentPerformed            ("event:media-persist:completed", PersistedContentEvent::class.java),
    ProcessCompleted              ("event:media-process:completed", MediaProcessCompletedEvent::class.java),
    ;

    companion object {
        fun toEvent(event: String): Events? {
            return Events.entries.find { it.event == event }
        }

        fun isOfWork(event: Events): Boolean {
            return event in listOf(

                WorkConvertCreated,
                WorkExtractCreated,
                WorkEncodeCreated,

                WorkEncodePerformed,
                WorkConvertPerformed,
                WorkExtractPerformed
            )
        }
    }
}