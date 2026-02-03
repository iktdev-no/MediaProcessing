package no.iktdev.mediaprocessing.transferModel.coordinatorUi

import java.time.Instant

data class CoordinatorHealth(
    val status: CoordinatorHealthStatus,
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


