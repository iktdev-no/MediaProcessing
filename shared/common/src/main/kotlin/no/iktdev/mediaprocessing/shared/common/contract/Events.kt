package no.iktdev.mediaprocessing.shared.common.contract

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import mu.KotlinLogging
import no.iktdev.eventi.core.LocalDateTimeAdapter
import no.iktdev.eventi.core.WGson
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import java.lang.reflect.Type
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}


enum class Events(val event: String) {
    ProcessStarted                ("event:media-process:started"),

    ReadStreamPerformed           ("event:media-read-stream:performed"             ),
    ParseStreamPerformed          ("event:media-parse-stream:performed"            ),
    ReadBaseInfoPerformed         ("event:media-read-base-info:performed"          ),
    MetadataSearchPerformed       ("event:media-metadata-search:performed"         ),
    ReadOutNameAndType            ("event:media-read-out-name-and-type:performed"  ),
    ReadOutCover                  ("event:media-read-out-cover:performed"          ),
    ParameterEncodeCreated        ("event:media-encode-parameter:created"          ),
    ParameterExtractCreated       ("event:media-extract-parameter:created"         ),
    WorkProceedPermitted          ("event:media-work-proceed:permitted"            ),
    WorkEncodeCreated                  ("event:work-encode:created"                ),
    WorkExtractCreated                 ("event:work-extract:created"               ),
    WorkConvertCreated                 ("event:work-convert:created"               ),
    WorkEncodePerformed                ("event:work-encode:performed"              ),
    WorkExtractPerformed               ("event:work-extract:performed"             ),
    WorkConvertPerformed               ("event:work-convert:performed"             ),
    WorkDownloadCoverPerformed         ("event:work-download-cover:performed"      ),
    PersistContentPerformed            ("event:media-persist:completed"            ),
    ProcessCompleted              ("event:media-process:completed"                 ),

    Unknown ("")
    ;

    companion object {
        fun toEvent(event: String): Events {
            return Events.entries.find { it.event == event } ?: Unknown
        }
    }
}

fun Events.toEventClass(): Class<out Event> {
    return when(this) {
        Events.ProcessStarted                -> MediaProcessStartEvent::class.java

        Events.ReadStreamPerformed           -> MediaFileStreamsReadEvent::class.java
        Events.ParseStreamPerformed          -> MediaFileStreamsParsedEvent::class.java
        Events.ReadBaseInfoPerformed         -> BaseInfoEvent::class.java
        Events.MetadataSearchPerformed       -> MediaMetadataReceivedEvent::class.java
        Events.ReadOutNameAndType            -> MediaOutInformationConstructedEvent::class.java
        Events.ReadOutCover                  -> MediaCoverInfoReceivedEvent::class.java

        Events.ParameterEncodeCreated        -> EncodeArgumentCreatedEvent::class.java
        Events.ParameterExtractCreated       -> ExtractArgumentCreatedEvent::class.java

        Events.WorkProceedPermitted          -> PermitWorkCreationEvent::class.java

        Events.WorkEncodeCreated             -> EncodeWorkCreatedEvent::class.java
        Events.WorkExtractCreated            -> ExtractWorkCreatedEvent::class.java
        Events.WorkConvertCreated            -> ConvertWorkCreatedEvent::class.java

        Events.WorkEncodePerformed           -> EncodeWorkPerformedEvent::class.java
        Events.WorkExtractPerformed          -> ExtractWorkPerformedEvent::class.java
        Events.WorkConvertPerformed          -> ConvertWorkPerformed::class.java
        Events.WorkDownloadCoverPerformed    -> MediaCoverDownloadedEvent::class.java

        Events.PersistContentPerformed       -> PersistedContentEvent::class.java
        Events.ProcessCompleted              -> MediaProcessCompletedEvent::class.java
        else -> Event::class.java
    }
}

fun String.jsonToEvent(eventType: String): Event {
    val event = Events.toEvent(eventType)
    return EventJson.fromJson(this, event)

    /*val clazz = Events.toEvent(eventType)?.toEventClass()
    clazz.let { eventClass ->
        try {
            val type = TypeToken.getParameterized(eventClass).type
            return WGson.gson.fromJson(this, type)
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
    log.error { "Failed to convert event: $eventType and data: $this to proper type!" }
    return WGson.gson.fromJson(this, type)*/
}

object EventJson {
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()
    fun fromJson(json: String, event: Events): Event {
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .registerTypeAdapter(Event::class.java, EventDeserializer(event))
            .create()
        return gson.fromJson(json, Event::class.java)
    }

    fun toJson(data: Any?): String {
        return gson.toJson(data)
    }

    class EventDeserializer(private val eventType: Events) : JsonDeserializer<Event> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Event {
            // 🔥 Finn riktig klasse basert på eventType (som kommer eksternt fra databasen)
            val eventClass = eventType.toEventClass()

            // Deserialiser objektet til riktig klasse
            val event = context.deserialize<Event>(json, eventClass)

            // 🔥 Sett eventType eksplisitt etter deserialisering
            if (event is Event) {
                event::class.java.getDeclaredField("eventType").apply {
                    isAccessible = true
                    set(event, eventType)
                }
            }

            return event
        }
    }


}
