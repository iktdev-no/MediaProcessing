package no.iktdev.mediaprocessing.transferModel.coordinatorUi

import java.time.Instant

data class SequenceSummary(
    val referenceId: String,
    val title: String,
    val inputFileName: String?,
    val type: ContextType = ContextType.Content,
    val lastEventId: String,
    val lastEventTime: Instant,
    val readStreamsTaskStatus: TaskStatus,
    val metadataTaskStatus: TaskStatus,
    val encodeTaskStatus: TaskStatus,
    val extractTaskStatus: TaskStatus,
    val convertTaskStatus: TaskStatus,
    val coverDownloadTaskStatus: TaskStatus,
    val contentMigratedTaskStatus: TaskStatus,
    val contentStoredTaskStatus: TaskStatus,
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