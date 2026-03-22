package no.iktdev.mediaprocessing.processer.segment

import no.iktdev.mediaprocessing.ffmpeg.model.AudioTrack
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.SegmentedEncodeTask

data class SegmentedRunnerContext(
    val task: SegmentedEncodeTask,
    val input: IFile,
    val output: IFile,
    val intermediateStore: IFile,
    val logDirectory: IFile,
    val videoCheckpointFile: IFile,
    val audioCheckpointFile: IFile,
    val taskStartTime: Long,
    val videoInstruction: FFmpegInstructions,
    val audioInstructions: List<FFmpegInstructions>,
)
