package no.iktdev.mediaprocessing.processer.segment

import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import java.io.File

data class SegmentedRunnerContext(
    val task: EncodeTask,
    val input: File,
    val output: File,
    val logDirectory: File,
    val checkpointFile: File,
    val taskStartTime: Long,
    val args: List<String>
)
