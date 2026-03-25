package no.iktdev.mediaprocessing.processer.progress

import mu.KotlinLogging
import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.EncodeProgress
import java.util.UUID

class LinearProgressListener(
    private val task: Task,
    private val reporter: TaskReporter?,
    private val weights: DynamicProgressWeights.Weights,
    cache: ((UUID, Progress) -> Unit)? = null
) : ProgressListener(task, reporter, cache) {

    private val log = KotlinLogging.logger {}

    fun onVideoProgress(decodedProgress: FfmpegDecodedProgress) {
        val local = decodedProgress.progress.toDouble() / 100.0
        val global = (local * weights.video) * 100

        report(global, decodedProgress, "Encoding video track")
    }

    fun onAudioProgress(index: Int, decodedProgress: FfmpegDecodedProgress, count: Int) {
        val local = decodedProgress.progress.toDouble() / 100.0
        val audioWeight = weights.audioPerTrack.take(index).sum()
        val global = ((weights.video + audioWeight) + local * weights.audioPerTrack[index]) * 100

        report(global, decodedProgress, "Encoding audio track ${index + 1}")
    }

    fun onMergeProgress(percent: Int) {
        val local = percent.toDouble() / 100.0

        // alt som er ferdig før merge
        val beforeMerge = weights.video + weights.audioPerTrack.sum()

        // global progress
        val global = (beforeMerge + local * weights.merge) * 100

        val decoded = FfmpegDecodedProgress(
            progress = percent,
            time = "",
            duration = "",
            speed = "",
            estimatedCompletion = "",
            estimatedCompletionSeconds = 0
        )

        report(global, decoded, "Merging audio and video")
    }


    override fun report(percent: Int, message: String) {
        log.error("${this.javaClass.simpleName} reported $percent% $message, but this listener has a different reporter")
    }

    fun report(taskProgress: Double, ffmpegDecodedProgress: FfmpegDecodedProgress, message: String) {
        val progress = EncodeProgress(
            progress = taskProgress.toInt(),
            ffmpegDecodedProgress = ffmpegDecodedProgress,
            message = message
        )
        cache?.invoke(task.taskId, progress)
        reporter?.updateProgress(task.referenceId, task.taskId, progress)
    }
}

