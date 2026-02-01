package no.iktdev.mediaprocessing.processer.listeners

import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg.Listener
import java.io.File

abstract class FfmpegTaskListener(taskType: TaskType): TaskListener(taskType) {
    open fun getFfmpeg(listener: Listener? = null, execPath: String, logDirectory: File): FFmpeg {
        return buildFfmpeg(
            listener = listener,
            execPath = execPath,
            logDirectory = logDirectory,
        ).also {
            it.onCreate()
        }
    }

    abstract fun buildFfmpeg(listener: Listener? = null, execPath: String, logDirectory: File): FFmpeg
}