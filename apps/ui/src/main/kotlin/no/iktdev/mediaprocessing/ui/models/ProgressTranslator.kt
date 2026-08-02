package no.iktdev.mediaprocessing.ui.models

import no.iktdev.eventi.serialization.ZPS.toProgress
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import no.iktdev.mediaprocessing.ui.models.contract.progress.EncodeProgress
import no.iktdev.mediaprocessing.ui.models.contract.progress.FileCopyProgress
import no.iktdev.mediaprocessing.ui.models.contract.progress.Progress
import no.iktdev.mediaprocessing.ui.models.contract.progress.SimpleProgress
import java.util.UUID

// Alias for felles-modellene så de ikke krasjer med UI-modellene
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.EncodeProgress as SharedEncodeProgress
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.FileCopyProgress as SharedFileCopyProgress
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress as SharedFfmpegDecodedProgress
import no.iktdev.mediaprocessing.ui.models.contract.progress.FfmpegDecodedProgress as UiFfmpegDecodedProgress

fun ProgressUpdate.translate(): Progress {
    val referenceId = UUID.fromString(this.referenceId)
    val taskId = UUID.fromString(this.taskId)
    return when (val e = this.envelope.toProgress()) {
        is SharedFileCopyProgress -> {
            FileCopyProgress(
                referenceId = referenceId,
                taskId = taskId,
                progress = e.progress,
                source = e.source,
                destination = e.destination,
            )
        }
        is SharedEncodeProgress -> {
            EncodeProgress(
                referenceId = referenceId,
                taskId = taskId,
                progress = e.progress,
                additionalInfo = e.ffmpegDecodedProgress.translate()
            )
        }
        else -> SimpleProgress(referenceId = referenceId, taskId = taskId, progress = e.progress)
    }
}

// Bruk aliaset her slik at det er 100% krystallklart hvilken som oversettes til hva
fun SharedFfmpegDecodedProgress.translate(): UiFfmpegDecodedProgress {
    return UiFfmpegDecodedProgress(
        time = this.time,
        duration = this.duration,
        speed = this.speed,
        estimatedCompletion = this.estimatedCompletion,
        estimatedCompletionSeconds = this.estimatedCompletionSeconds,
    )
}