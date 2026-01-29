package no.iktdev.mediaprocessing.shared.common.dto

import java.time.Instant
import java.util.*

data class ResetTaskResponse(
    val taskId: UUID,
    val referenceId: UUID,
    val deletedEventId: UUID?,
    val status: String,
    val resetAt: Instant
)
