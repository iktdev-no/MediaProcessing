package no.iktdev.mediaprocessing.ui.models.contract.sequence

import no.iktdev.mediaprocessing.ui.models.contract.TaskStatus
import java.time.Instant

data class Sequence(
    val referenceId: String,
    val title: String,
    val inputFileName: String?,
    val type: ContextType = ContextType.Content,
    val lastEventId: String,
    val lastEventTime: Instant,
    val tasks: Map<TaskType, TaskStatus>,
    val mode: Mode,
    val currentState: CurrentState,
    val hasErrors: Boolean,
)

enum class TaskType {
    ReadStreams,
    MetadataSearch,
    Encode,
    SubtitleExtract,
    SubtitleConvert,
    CoverDownload,
    ContentPersist,
    MediaInfoStored
}

enum class CurrentState {
    Continuing,
    OnHold
}

enum class Mode {
    Auto,
    Manual
}

enum class ContextType {
    Content,
    Metadata
}