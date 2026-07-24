package no.iktdev.mediaprocessing.shared.common.dto

import java.time.Instant
import java.util.*

data class IgnoredTaskResponse(
    val taskId: UUID,
    val referenceId: UUID,
    val deletedEventId: UUID?,
    val skippedEventId: UUID?,
    val skipped: Boolean,
    val ignoredAt: Instant
)
