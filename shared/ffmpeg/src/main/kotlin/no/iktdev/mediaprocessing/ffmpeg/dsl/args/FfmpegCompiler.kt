package no.iktdev.mediaprocessing.ffmpeg.dsl.args

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.*
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.StreamType.*

data class StreamKey(
    val inputIndex: Int,
    val type: StreamType,
    val streamIndex: Int
)

class FfmpegCompiler(
    private val inputs: InputSection,
    private val output: OutputSection?,
    private val segment: SegmentSection? = null,
    private val outStore: IFile? = null
) {

    fun compile(): List<String> {
        val inputFiles = inputs.files()
        val concatFile = inputs.concatInput

        require(inputFiles.isNotEmpty() || concatFile != null) { "At least one input is required" }

        val args = if (concatFile != null) {
            FfmpegSubcompilerConcat(output = output).compile(concatFile).toMutableList()
                .apply { compileOutput(this) }
        } else if (inputFiles.isNotEmpty()) {
            FfmpegSubcompilerDefault(
                inputs = inputs,
                output = output,
                segment = segment,
                outStore = outStore
            ).compile().toMutableList().apply { compileOutput(this) }
        } else {
            error("No inputs were found")
        }
        return args
    }


    // ---------------------------------------------------------
    // SHARED HELPERS
    // ---------------------------------------------------------

    private fun compileOutput(args: MutableList<String>) {
        val out = requireNotNull(output)
        val outFile = outStore?.using(out.resolvedName()) ?: IFile(out.resolvedName())
        args += outFile.absolutePath
        if (out.progress) args += listOf("-progress", "pipe:1")
    }

}
