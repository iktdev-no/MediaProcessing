package no.iktdev.mediaprocessing.ffmpeg.dsl.args

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.ConcatInputConfig
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputConfig
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.SegmentSection

class FfmpegDsl {

    private var inputSection = InputSection()
    private var outputSection: OutputSection? = null
    private var segmentSection: SegmentSection? = null
    private var storeDirectory: IFile? = null


    fun toInstructions(): FFmpegInstructions =
        FFmpegInstructions(
            inputs = this.inputSection,
            output = this.outputSection
        )

    fun fromInstructions(instruct: FFmpegInstructions) {
        this.inputSection = instruct.inputs
        this.outputSection = instruct.output
    }

    // ---------------------------------------------------------
    // INPUT
    // ---------------------------------------------------------
    fun input(path: String, block: InputConfig.() -> Unit) {
        inputSection.file(path, block)
    }

    fun concatFile(path: String, block: ConcatInputConfig.() -> Unit) {
        inputSection.concat(path, block)
    }

    // ---------------------------------------------------------
    // OUTPUT
    // ---------------------------------------------------------
    fun output(path: String, block: OutputSection.() -> Unit = {}) {
        outputSection = OutputSection(path).apply(block)
    }

    fun outputDirectory(dir: IFile) {
        this.storeDirectory = dir
    }

    // ---------------------------------------------------------
    // SEGMENT
    // ---------------------------------------------------------
    fun segment(start: Double? = null, duration: Double? = null) {
        segmentSection = SegmentSection().apply {
            this.start = start
            this.duration = duration
        }
    }

    // ---------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------
    fun outputFile(): String {
        val fileName = outputSection?.path ?: error("Output must be defined")
        return storeDirectory?.using(fileName)?.absolutePath ?: fileName
    }

    fun outputWorkFile(): String {
        val fileName = outputSection?.workFile ?: error("Output must be defined")
        return storeDirectory?.using(fileName)?.absolutePath ?: fileName
    }

    fun outputFileUsed(): String {
        val fileName = outputSection?.resolvedName() ?: error("Output must be defined")
        return storeDirectory?.using(fileName)?.absolutePath ?: fileName
    }

    fun isUsingWorkFile(): Boolean =
        outputSection?.useWorkFile ?: error("Output must be defined")

    fun overwrite(): Boolean = outputSection?.overwrite ?: false

    // ---------------------------------------------------------
    // BUILD
    // ---------------------------------------------------------
    fun build(): List<String> =
        FfmpegCompiler(
            inputs = inputSection,
            output = outputSection,
            segment = segmentSection,
            outStore = storeDirectory
        ).compile()
}

fun ffmpeg(block: FfmpegDsl.() -> Unit): FfmpegDsl =
    FfmpegDsl().apply(block)
