package no.iktdev.mediaprocessing.ffmpeg.dsl.args

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.ConcatInputConfig
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.SegmentSection

class FfmpegSubcompilerConcat(
    private val output: OutputSection?,
) {
    fun compile(concat: ConcatInputConfig): List<String> {
        val args = mutableListOf<String>()
        val out = requireNotNull(output)

        if (out.overwrite) args += "-y"
        args += listOf("-nostdin", "-nostats", "-hide_banner")

        args += listOf("-f", "concat", "-safe", "0", "-i", concat.listFile)
        args += listOf("-c", "copy")

        return args
    }

}