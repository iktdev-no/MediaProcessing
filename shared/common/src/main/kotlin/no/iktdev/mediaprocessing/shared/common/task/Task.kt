package no.iktdev.mediaprocessing.shared.common.task

import java.time.LocalDateTime

data class Task(
    val referenceId: String,
    val status: String? = null,
    val claimed: Boolean = false,
    val claimedBy: String? = null,
    val consumed: Boolean = false,
    val task: TaskType,
    val eventId: String,
    val derivedFromEventId: String? = null,
    val data: TaskData? = null,
    val created: LocalDateTime,
    val lastCheckIn: LocalDateTime? = null
)
