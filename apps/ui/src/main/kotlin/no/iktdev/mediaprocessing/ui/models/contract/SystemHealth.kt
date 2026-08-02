package no.iktdev.mediaprocessing.ui.models.contract

import no.iktdev.mediaprocessing.ui.models.contract.sequence.SequenceHealth
import java.time.Instant

data class SystemHealth(
    val status: SystemHealthStatus,
    val abandonedTasks: Int,
    val stalledTasks: Int,
    val activeTasks: Int,
    val queuedTasks: Int,
    val failedTasks: Int,
    val sequencesOnHold: Int,
    val lastActivity: Instant?,

    // IDs for UI linking
    val abandonedTaskIds: List<String>,
    val stalledTaskIds: List<String>,
    val sequencesOnHoldIds: List<String>,
    val overdueSequenceIds: List<String>,


    // Detailed sequence info
    val overdueSequences: List<SequenceHealth>,

    val details: Map<String, Any?>
)

enum class SystemHealthStatus {
    HEALTHY,
    DEGRADED,
    UNHEALTHY
}