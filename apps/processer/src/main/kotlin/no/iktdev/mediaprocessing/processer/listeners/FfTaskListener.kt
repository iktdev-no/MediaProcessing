package no.iktdev.mediaprocessing.processer.listeners

import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg.Listener
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.files.IFile
import java.io.File

abstract class FfTaskListener(taskType: TaskType, val execConfig: ExecutablesConfig): TaskListener(taskType), FfProvider {
    override fun getFfmpeg(listener: Listener?, logDirectory: IFile): FFmpeg {
        return buildFfmpeg(
            listener = listener,
            execPath = execConfig.ffmpeg,
            logDirectory = logDirectory,
        ).also {
            it.onCreate()
        }
    }

    override fun getExecutableFfprobe(): String = execConfig.ffprobe

    abstract fun buildFfmpeg(listener: Listener? = null, execPath: String, logDirectory: IFile): FFmpeg

    class FfmpegFailedException(
        val logFile: IFile? = null,
        message: String
    ) : RuntimeException(message)

}