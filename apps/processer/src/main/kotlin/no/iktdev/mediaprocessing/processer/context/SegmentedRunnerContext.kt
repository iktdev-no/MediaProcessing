package no.iktdev.mediaprocessing.processer.context

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.SegmentedEncodeTask

data class SegmentedRunnerContext(
    override val task: SegmentedEncodeTask,
    override val input: IFile,
    val output: IFile,
    override val intermediateStore: IFile,
    override val logDirectory: IFile,
    val videoCheckpointFile: IFile,
    val audioCheckpointFile: IFile,
    override val taskStartTime: Long,
    override val videoInstruction: FFmpegInstructions,
    override val audioInstructions: List<FFmpegInstructions>,
) : RunnerContext(
    task = task, input = input, intermediateStore = intermediateStore,
    logDirectory = logDirectory,
    taskStartTime = taskStartTime,
    videoInstruction = videoInstruction,
    audioInstructions = audioInstructions,
)