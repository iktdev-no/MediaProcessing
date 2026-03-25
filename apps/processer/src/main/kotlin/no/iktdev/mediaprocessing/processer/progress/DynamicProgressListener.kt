package no.iktdev.mediaprocessing.processer.progress

import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.EncodeProgress
import java.util.UUID

open class DynamicProgressListener(
    private val task: Task,
    private val reporter: TaskReporter?,
    private val weights: DynamicProgressWeights.Weights,
    cache: ((UUID, Progress) -> Unit)? = null
): ProgressListener(task = task, reporter = reporter, cache = cache) {

    fun onVideoProgress(done: Double, total: Double) {
        val local = done / total
        val global = (local * weights.video) * 100
        report(global.toInt(), "Encoding video")
    }

    fun onAudioProgress(trackIndex: Int, done: Double, total: Double) {
        val local = done / total
        val audioWeight = weights.audioPerTrack.take(trackIndex).sum()
        val global = ((weights.video + audioWeight) + local * weights.audioPerTrack[trackIndex]) * 100
        report(global.toInt(), "Encoding audio track ${trackIndex + 1}")
    }

    fun onMergeProgress(local: Double) {
        val beforeMerge = weights.video + weights.audioPerTrack.sum()
        val global = (beforeMerge + local * weights.merge) * 100
        report(global.toInt(), "Merging audio and video")
    }

    override fun report(percent: Int, message: String) {
        val progress = EncodeProgress(
            progress = percent,
            ffmpegDecodedProgress = FfmpegDecodedProgress(
                progress = percent,
                time = "",
                duration = "",
                speed = "",
                estimatedCompletion = "",
                estimatedCompletionSeconds = 0
            ),
            message = message
        )
        cache?.invoke(task.taskId, progress)
        reporter?.updateProgress(task.referenceId, task.taskId, progress)
    }
}
