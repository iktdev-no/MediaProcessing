package no.iktdev.mediaprocessing.processer.listeners

import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg

abstract class FfmpegTaskListener(taskType: TaskType): TaskListener(taskType) {
    abstract fun getFfmpeg(): FFmpeg
}