package no.iktdev.mediaprocessing.processer.listeners

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent

abstract class VideoTaskListener(taskType: TaskType, execConfig: ExecutablesConfig): FfTaskListener(taskType, execConfig) {
    private val log = KotlinLogging.logger {}

    override fun createIncompleteStateTaskEvent(
        task: Task,
        status: TaskStatus,
        exception: Exception?
    ): Event {
        val message = when (status) {
            TaskStatus.Failed -> exception?.message ?: "Unknown error, see log"
            TaskStatus.Cancelled -> "Canceled"
            else -> ""
        }
        val logFile = if (exception is FfmpegFailedException) exception.logFile?.absolutePath else null
        return ProcesserEncodeResultEvent(null, logFile, status, error = message).producedFrom(task)
    }

    override fun buildFfmpeg(listener: FFmpeg.Listener?, execPath: String, logDirectory: IFile): FFmpeg {
        return VideoFFmpeg(execPath = execPath,
            logDirectory = logDirectory,
            listener = listener)
    }

    class VideoFFmpeg(
        override val listener: Listener? = null,
        private val execPath: String,
        val logDirectory: IFile
    ) : FFmpeg(executable = execPath, logDir = logDirectory) {

        override fun onCreate() {
            super.onCreate()
            if (!logDirectory.exists()) {
                logDirectory.mkdirs()
            }
        }
    }

}