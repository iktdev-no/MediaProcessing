package no.iktdev.mediaprocessing.processer.progress

import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.EncodeProgress
import java.util.UUID

class SegmentedProgressListener(
    private val task: Task,
    private val reporter: TaskReporter?,
): ProgressListener(task = task, reporter = reporter) {

    private val VIDEO_WEIGHT = 0.65
    private val CONCAT_WEIGHT = 0.05
    private val AUDIO_WEIGHT = 0.25
    private val MERGE_WEIGHT = 0.05

    fun onVideoProgress(doneSeconds: Double, totalSeconds: Double) {
        val local = doneSeconds / totalSeconds
        val global = (local * VIDEO_WEIGHT) * 100
        report(global.toInt(), "Encoding video segments")
    }

    fun onConcatProgress(local: Double) {
        val global = (VIDEO_WEIGHT + local * CONCAT_WEIGHT) * 100
        report(global.toInt(), "Concatenating video segments")
    }

    fun onAudioProgress(doneTracks: Int, totalTracks: Int) {
        val local = doneTracks.toDouble() / totalTracks
        val global = (VIDEO_WEIGHT + CONCAT_WEIGHT + local * AUDIO_WEIGHT) * 100
        report(global.toInt(), "Encoding audio tracks")
    }

    fun onMergeProgress(local: Double) {
        val global = (VIDEO_WEIGHT + CONCAT_WEIGHT + AUDIO_WEIGHT + local * MERGE_WEIGHT) * 100
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
        reporter?.updateProgress(task.referenceId, task.taskId, progress)
    }
}