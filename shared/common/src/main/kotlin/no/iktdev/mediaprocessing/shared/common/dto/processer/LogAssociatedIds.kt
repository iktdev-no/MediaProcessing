package no.iktdev.mediaprocessing.shared.common.dto.processer

import java.util.UUID

data class LogAssociatedIds(
    val referenceId: UUID,
    val ids: Set<UUID>,
    val logFile: String
)