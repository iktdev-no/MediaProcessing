package no.iktdev.mediaprocessing.shared.common.dto

import java.time.Instant
import java.util.*

data class ResetTaskResponse(
    val taskId: UUID,
    val referenceId: UUID,
    val deletedEventId: UUID?,
    val reset: Boolean,
    val resetAt: Instant
)
