package no.iktdev.eventi.data

import com.google.gson.Gson
import no.iktdev.eventi.core.WGson
import java.time.LocalDateTime
import java.util.*

abstract class EventImpl {
    abstract val metadata: EventMetadata
    abstract val data: Any?
    abstract val eventType: Any
}

fun <T> EventImpl.dataAs(): T? {
    return this.data as T
}

fun EventImpl.toJson(): String {
    return WGson.gson.toJson(this)
}

data class EventMetadata(
    val derivedFromEventId: String? = null, // Can be null but should not, unless its init event
    val eventId: String = UUID.randomUUID().toString(),
    val referenceId: String,
    val status: EventStatus,
    val created: LocalDateTime = LocalDateTime.now(),
    val source: String = "Unknown producer"
)

enum class EventStatus {
    Success,
    Skipped,
    Failed
}

fun EventImpl.isSuccessful(): Boolean {
    return this.metadata.status == EventStatus.Success
}

fun EventImpl.isSkipped(): Boolean {
    return this.metadata.status == EventStatus.Skipped
}

fun EventImpl.isFailed(): Boolean {
    return this.metadata.status == EventStatus.Failed
}

fun EventImpl.referenceId(): String {
    return this.metadata.referenceId
}

fun EventImpl.eventId(): String {
    return this.metadata.eventId
}

fun EventImpl.derivedFromEventId(): String? {
    return this.metadata.derivedFromEventId
}