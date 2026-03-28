package no.iktdev.mediaprocessing.processer.models

import java.util.UUID

data class ProcessEntry(
    val pid: Long,
    val taskId: UUID,
    val processType: ProcessType
)

enum class ProcessType {
    LINEAR_VIDEO_ENCODE,
    LINEAR_AUDIO_ENCODE,
    SEGMENTED_VIDEO_ENCODE,
    SEGMENTED_AUDIO_ENCODE,
}