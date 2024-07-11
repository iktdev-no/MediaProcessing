package no.iktdev.eventi.data

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


data class EventMetadata(
    val referenceId: String,
    val eventId: String = UUID.randomUUID().toString(),
    val derivedFromEventId: String? = null, // Can be null but should not, unless its init event
    val status: EventStatus,
    val created: LocalDateTime = LocalDateTime.now()
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
