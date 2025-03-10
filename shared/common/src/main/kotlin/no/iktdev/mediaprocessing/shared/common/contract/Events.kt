package no.iktdev.mediaprocessing.shared.common.contract

import com.google.gson.*
import mu.KotlinLogging
import no.iktdev.eventi.core.LocalDateTimeAdapter
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import java.lang.reflect.Type
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}


enum class Events {
    ProcessStarted,
    StreamRead,
    StreamParsed,
    BaseInfoRead,
    MetadataSearchPerformed,
    ReadOutNameAndType,
    ReadOutCover,
    EncodeParameterCreated,
    ExtractParameterCreated,
    WorkProceedPermitted,
    EncodeTaskCreated,
    ExtractTaskCreated,
    ConvertTaskCreated,
    EncodeTaskCompleted,
    ExtractTaskCompleted,
    ConvertTaskCompleted,
    CoverDownloaded,
    PersistContent,
    ProcessCompleted,

    Unknown
    ;

    companion object {
        fun toEvent(event: String): Events {
            return Events.entries.find { it.name == event } ?: Unknown
        }
    }
}

fun Events.toEventClass(): Class<out Event> {
    return when (this) {
        Events.ProcessStarted -> MediaProcessStartEvent::class.java

        Events.StreamRead -> MediaFileStreamsReadEvent::class.java
        Events.StreamParsed -> MediaFileStreamsParsedEvent::class.java
        Events.BaseInfoRead -> BaseInfoEvent::class.java
        Events.MetadataSearchPerformed -> MediaMetadataReceivedEvent::class.java
        Events.ReadOutNameAndType -> MediaOutInformationConstructedEvent::class.java
        Events.ReadOutCover -> MediaCoverInfoReceivedEvent::class.java

        Events.EncodeParameterCreated -> EncodeArgumentCreatedEvent::class.java
        Events.ExtractParameterCreated -> ExtractArgumentCreatedEvent::class.java

        Events.WorkProceedPermitted -> PermitWorkCreationEvent::class.java

        Events.EncodeTaskCreated -> EncodeWorkCreatedEvent::class.java
        Events.ExtractTaskCreated -> ExtractWorkCreatedEvent::class.java
        Events.ConvertTaskCreated -> ConvertWorkCreatedEvent::class.java

        Events.EncodeTaskCompleted -> EncodeWorkPerformedEvent::class.java
        Events.ExtractTaskCompleted -> ExtractWorkPerformedEvent::class.java
        Events.ConvertTaskCompleted -> ConvertWorkPerformed::class.java
        Events.CoverDownloaded -> MediaCoverDownloadedEvent::class.java

        Events.PersistContent -> PersistedContentEvent::class.java
        Events.ProcessCompleted -> MediaProcessCompletedEvent::class.java
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

            if (eventClass.simpleName == Event::class.java.simpleName || eventType == Events.Unknown) {
                val fallbackGson = GsonBuilder()
                    .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
                    .create()

                return fallbackGson.fromJson(json, eventClass)
            }
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
