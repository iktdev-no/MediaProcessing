package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.ffmpeg.FFprobe

abstract class FfprobeTaskListener(taskType: TaskType): TaskListener(taskType) {
    abstract fun getFfprobe(): FFprobe
}