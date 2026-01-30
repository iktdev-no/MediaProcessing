package no.iktdev.mediaprocessing.shared.common.dto

import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import java.time.Instant

data class SequenceSummary(
    val referenceId: String,
    val title: String,
    val inputFileName: String?,
    val type: ContextType = ContextType.Content,
    val lastEventId: String,
    val lastEventTime: Instant,
    val metadataTaskStatus: CollectProjection.TaskStatus,
    val encodeTaskStatus: CollectProjection.TaskStatus,
    val extractTaskStatus: CollectProjection.TaskStatus,
    val convertTaskStatus: CollectProjection.TaskStatus,
    val coverDownloadTaskStatus: CollectProjection.TaskStatus,
    val contentMigratedTaskStatus: CollectProjection.TaskStatus,
    val contentStoredTaskStatus: CollectProjection.TaskStatus,
    val mode: Mode,
    val currentState: CurrentState,
    val hasErrors: Boolean,
)

enum class ContextType {
    Content,
    Metadata
}

enum class Mode {
    Auto,
    Manual
}

enum class CurrentState {
    Continuing,
    OnHold
}