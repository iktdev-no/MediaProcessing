package no.iktdev.mediaprocessing.ffmpeg.data

import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.ConcatInputConfig
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputConfig
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection

data class FFmpegInstructions(
    val inputs: InputSection,
    val output: OutputSection?
) {
    fun findPrimaryInput(): String {
        val all = this.inputs.inputs

        return when {
            all.any { it is InputConfig } ->
                all.filterIsInstance<InputConfig>().first().path

            all.any { it is ConcatInputConfig } ->
                all.filterIsInstance<ConcatInputConfig>().first().listFile

            else -> error("No valid input found")
        }
    }
}
