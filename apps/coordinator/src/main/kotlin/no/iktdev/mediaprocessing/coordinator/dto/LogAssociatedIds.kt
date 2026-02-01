package no.iktdev.mediaprocessing.coordinator.dto

import java.util.*

data class LogAssociatedIds(
    val referenceId: UUID,
    val ids: Set<UUID>,
    val logFile: String
)