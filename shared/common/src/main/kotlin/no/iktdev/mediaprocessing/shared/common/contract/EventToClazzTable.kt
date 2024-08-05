package no.iktdev.mediaprocessing.shared.common.contract

import com.google.gson.reflect.TypeToken
import mu.KotlinLogging
import no.iktdev.eventi.core.WGson
import no.iktdev.mediaprocessing.shared.common.contract.data.*

private val log = KotlinLogging.logger {}
object EventToClazzTable {

    val table = mutableMapOf(
        Events.EventMediaProcessStarted to MediaProcessStartEvent::class.java,
        Events.EventMediaReadBaseInfoPerformed to BaseInfoEvent::class.java,
        Events.EventMediaReadStreamPerformed to MediaFileStreamsReadEvent::class.java,
        Events.EventMediaParseStreamPerformed to MediaFileStreamsParsedEvent::class.java,
        Events.EventWorkConvertCreated to ConvertWorkCreatedEvent::class.java,
        Events.EventWorkConvertPerformed to ConvertWorkPerformed::class.java,
        Events.EventMediaParameterEncodeCreated to EncodeArgumentCreatedEvent::class.java,
        Events.EventWorkEncodeCreated to EncodeWorkCreatedEvent::class.java,
        Events.EventWorkEncodePerformed to EncodeWorkPerformedEvent::class.java,
        Events.EventMediaParameterExtractCreated to ExtractArgumentCreatedEvent::class.java,
        Events.EventWorkExtractCreated to ExtractWorkCreatedEvent::class.java,
        Events.EventWorkExtractPerformed to ExtractWorkPerformedEvent::class.java,
        Events.EventWorkDownloadCoverPerformed to MediaCoverDownloadedEvent::class.java,
        Events.EventMediaReadOutCover to MediaCoverInfoReceivedEvent::class.java,
        Events.EventMediaParseStreamPerformed to MediaFileStreamsParsedEvent::class.java,
        Events.EventMediaReadStreamPerformed to MediaFileStreamsReadEvent::class.java,
        Events.EventMediaMetadataSearchPerformed to MediaMetadataReceivedEvent::class.java,
        Events.EventMediaReadOutNameAndType to MediaOutInformationConstructedEvent::class.java,
        Events.EventMediaWorkProceedPermitted to PermitWorkCreationEvent::class.java,
        Events.EventMediaProcessCompleted to MediaProcessCompletedEvent::class.java
    )

}

fun String.fromJsonWithDeserializer(event: Events): Event {
    val clazz = EventToClazzTable.table[event]
    clazz?.let { eventClass ->
        try {
            val type = TypeToken.getParameterized(eventClass).type
            return WGson.gson.fromJson<Event>(this, type)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    try {
        // Fallback
        val type = object : TypeToken<Event>() {}.type
        return WGson.gson.fromJson(this, type)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Default
    val type = object : TypeToken<Event>() {}.type
    log.error { "Failed to convert event: $event and data: $this to proper type!" }
    return WGson.gson.fromJson<Event>(this, type)

}