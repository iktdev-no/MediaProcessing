package no.iktdev.mediaprocessing.processer.context

import no.iktdev.eventi.models.Task
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions

open class RunnerContext(
    open val task: Task,
    open val input: IFile,
    open val intermediateStore: IFile,
    open val logDirectory: IFile,
    open val taskStartTime: Long,
    open val videoInstruction: FFmpegInstructions,
    open val audioInstructions: List<FFmpegInstructions>,
) {
}