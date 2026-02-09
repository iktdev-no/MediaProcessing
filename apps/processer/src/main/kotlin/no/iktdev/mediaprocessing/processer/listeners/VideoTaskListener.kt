package no.iktdev.mediaprocessing.processer.listeners

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.processer.config.ProcesserProperties
import no.iktdev.mediaprocessing.processer.strategy.EncodingStrategy
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import org.jetbrains.annotations.VisibleForTesting
import java.io.File

abstract class VideoTaskListener(taskType: TaskType, private val processerProperties: ProcesserProperties): FfmpegTaskListener(taskType) {

    override fun supports(task: Task) = task is EncodeTask

    override fun accept(task: Task, reporter: TaskReporter): Boolean {
        if (!supports(task)) return false
        task as EncodeTask

        val determinedStrategy = getEncodeStrategy(task)
        val strategy = if (!processerProperties.enableSegmentedTaskListener && determinedStrategy == EncodingStrategy.Segmented)
            EncodingStrategy.Linear
        else determinedStrategy

        return when (strategy) {
            EncodingStrategy.Segmented ->
                if (this is SegmentedVideoTaskListener) super.accept(task, reporter) else false

            EncodingStrategy.Linear ->
                if (this is LinearVideoTaskListener) super.accept(task, reporter) else false
        }
    }


    @VisibleForTesting
    internal fun getEncodeStrategy(task: EncodeTask): EncodingStrategy {
        val args = task.data.arguments ?: emptyList()

        fun containsAll(vararg tokens: String): Boolean =
            tokens.all { args.contains(it) }

        fun hasFlagWithValue(flag: String, value: String): Boolean {
            val index = args.indexOf(flag)
            return index >= 0 && index + 1 < args.size && args[index + 1] == value
        }

        val touchesVideo = args.any { it.startsWith("-c:v") || it == "-vf" || it == "-filter_complex" }
        val touchesAudio = args.any { it.startsWith("-c:a") || it == "-af" }

        // 1. Pure copy → Linear
        if (hasFlagWithValue("-c", "copy") ||
            hasFlagWithValue("-c:v", "copy") ||
            hasFlagWithValue("-c:a", "copy")
        ) {
            return EncodingStrategy.Linear
        }

        // 2. Concat → Linear
        if (containsAll("-f", "concat"))
            return EncodingStrategy.Linear

        // 3. Seek-before-input → Linear
        if (args.contains("-ss") && args.indexOf("-ss") < args.indexOf("-i"))
            return EncodingStrategy.Linear

        // 4. Trim → Linear
        if (args.contains("-t") || args.contains("-to"))
            return EncodingStrategy.Linear

        // 5. Audio-only re-encode → Linear
        if (touchesAudio && !touchesVideo)
            return EncodingStrategy.Linear

        // 6. Video re-encode or filtergraph → Segmented
        if (touchesVideo)
            return EncodingStrategy.Segmented

        // 7. Default: Linear (safe fallback)
        return EncodingStrategy.Linear
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

    override fun buildFfmpeg(listener: FFmpeg.Listener?, execPath: String, logDirectory: File): FFmpeg {
        return VideoFFmpeg(execPath = execPath,
            logDirectory = logDirectory,
            listener = listener)
    }

    class VideoFFmpeg(
        override val listener: Listener? = null,
        private val execPath: String,
        val logDirectory: File
    ) : FFmpeg(executable = execPath, logDir = logDirectory) {

        override fun onCreate() {
            super.onCreate()
            if (!logDirectory.exists()) {
                logDirectory.mkdirs()
            }
        }
    }

}