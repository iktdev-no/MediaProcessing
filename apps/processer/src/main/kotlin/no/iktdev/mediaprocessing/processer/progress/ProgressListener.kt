package no.iktdev.mediaprocessing.processer.progress

import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.tasks.TaskReporter
import java.util.UUID

abstract class ProgressListener(
    private val task: Task,
    private val reporter: TaskReporter?,
) {


    abstract fun report(percent: Int, message: String)
}