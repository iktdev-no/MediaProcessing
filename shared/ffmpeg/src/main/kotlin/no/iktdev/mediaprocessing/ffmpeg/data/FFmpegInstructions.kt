package no.iktdev.mediaprocessing.ffmpeg.data

import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection

data class FFmpegInstructions(
    val inputs: InputSection,
    val output: OutputSection?
) {
    fun findPrimaryInput(): String {
        val inputs = inputs
        val concatInput = inputs.concatInput
        if (concatInput != null) return concatInput.listFile

        return inputs.files().firstOrNull()?.path ?: error("No inputs")
    }

}
