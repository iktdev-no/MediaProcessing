package no.iktdev.mediaprocessing.processer.listeners

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent

abstract class VideoTaskListener(taskType: TaskType, execConfig: ExecutablesConfig) :
    FfTaskListener(taskType, execConfig) {
    private val log = KotlinLogging.logger {}

    companion object {
        @Volatile
        private var sharedListeners: MutableList<VideoTaskListener> = mutableListOf()
        fun addListener(listener: VideoTaskListener) {
            sharedListeners.add(listener)
        }

        fun removeListeners() {
            sharedListeners.clear()
        }

        var useSharedBusyState: Boolean = false
            private set

        fun setUseSharedBusyState(use: Boolean) {
            useSharedBusyState = use
        }
    }

    init {
        addListener(this)
    }

    override val isBusy: Boolean
        get() = if (useSharedBusyState && anySharedListenersBusy()) true else super.isBusy

    private fun anySharedListenersBusy(): Boolean {
        val busyListener = sharedListeners
            .filter { it !== this }
            .filter { it.currentJob?.isActive == true }

        if (busyListener.isEmpty()) {
            return false
        }

        val holder = busyListener.first().javaClass.simpleName
        log.info { "Busy signal retrieved from $holder" }

        return true
    }


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
        return VideoFFmpeg(
            execPath = execPath,
            logDirectory = logDirectory,
            listener = listener
        )
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