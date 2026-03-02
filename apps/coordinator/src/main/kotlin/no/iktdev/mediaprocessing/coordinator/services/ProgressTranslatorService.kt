package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.serialization.ZPS.toProgress
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.EncodeProgress
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.FileCopyProgress
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress.Progress
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress.SimpleProgress
import org.springframework.stereotype.Service

@Service
class ProgressTranslatorService {

    fun translate(progress: ProgressUpdate): Progress {
        return when (val prog = progress.envelope.toProgress()) {
            is EncodeProgress -> {
                no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress.EncodeProgress(
                    referenceId = progress.referenceId,
                    taskId = progress.taskId,
                    progress = prog.progress,
                    additionalInfo = FfmpegDecodedProgress(
                        time = prog.ffmpegDecodedProgress.time,
                        duration = prog.ffmpegDecodedProgress.duration,
                        speed = prog.ffmpegDecodedProgress.speed,
                        estimatedCompletion = prog.ffmpegDecodedProgress.estimatedCompletion,
                        estimatedCompletionSeconds = prog.ffmpegDecodedProgress.estimatedCompletionSeconds
                    ),
                )
            }
            is FileCopyProgress -> {
                no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress.FileCopyProgress(
                    referenceId = progress.referenceId,
                    taskId = progress.taskId,
                    progress = prog.progress,
                    source = prog.source,
                    destination = prog.destination,
                )
            }

            else -> {
                SimpleProgress(referenceId = progress.referenceId, taskId = progress.taskId, progress = prog.progress)
            }
        }
    }
}