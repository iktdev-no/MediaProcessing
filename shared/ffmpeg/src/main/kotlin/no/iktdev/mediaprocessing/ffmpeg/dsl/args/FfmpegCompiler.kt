package no.iktdev.mediaprocessing.ffmpeg.dsl.args

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.*
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.StreamType.*

private data class StreamKey(
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

        if (concatFile != null) return compileConcat(concatFile)
        return compileNormal(inputFiles)
    }

    // ---------------------------------------------------------
    // NORMAL MODE
    // ---------------------------------------------------------
    private fun compileNormal(inputs: List<InputConfig>): List<String> {
        val args = mutableListOf<String>()
        val out = requireNotNull(output)

        // global flags
        if (out.overwrite) args += "-y"
        args += listOf("-nostdin", "-nostats", "-hide_banner")

        // strip data streams
        args += "-dn"

        // segment trimming
        segment?.start?.let { args += listOf("-ss", it.toString()) }
        segment?.duration?.let { args += listOf("-t", it.toString()) }

        // inputs
        inputs.forEach { input ->
            args += listOf("-i", input.path)
        }

        // mapping
        val mappingInfo = compileMapping(inputs, args)

        // metadata
        compileMetadata(inputs, mappingInfo, args)

        // codecs
        compileCodecs(inputs, mappingInfo, args)

        // output
        compileOutput(args)

        return args
    }

    // ---------------------------------------------------------
    // CONCAT MODE
    // ---------------------------------------------------------
    private fun compileConcat(concat: ConcatInputConfig): List<String> {
        val args = mutableListOf<String>()
        val out = requireNotNull(output)

        if (out.overwrite) args += "-y"
        args += listOf("-nostdin", "-nostats", "-hide_banner")

        args += listOf("-f", "concat", "-safe", "0", "-i", concat.listFile)
        args += listOf("-c", "copy")

        compileOutput(args)
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

    private fun compileMetadata(
        inputs: List<InputConfig>,
        mapping: MappingInfo,
        args: MutableList<String>
    ) {
        val onlySubtitles =
            inputs.all { input -> input.allStreams().all { it is SubtitleStreamConfig } && inputs.size == 1 }

        inputs.forEachIndexed { inputIndex, input ->
            input.allStreams().forEach { stream ->

                if (stream is AudioStreamConfig) {
                    val key = StreamKey(inputIndex, AUDIO, stream.streamIndex)
                    val outIndex = mapping.audioOutIndexMap[key] ?: stream.streamIndex

                    stream.language?.let { args += listOf("-metadata:s:a:$outIndex", "language=$it") }
                    stream.title?.let { args += listOf("-metadata:s:a:$outIndex", "title=$it") }
                    if (stream.default) args += listOf("-disposition:a:$outIndex", "default")
                    if (stream.forced) args += listOf("-disposition:a:$outIndex", "forced")
                    if (stream.commentary) args += listOf("-metadata:s:a:$outIndex", "commentary=1")
                    if (stream.descriptive) args += listOf("-metadata:s:a:$outIndex", "audesc=1")
                    if (stream.hearingImpaired) args += listOf("-metadata:s:a:$outIndex", "hearing_impaired=1")
                    if (stream.original) args += listOf("-metadata:s:a:$outIndex", "original=1")
                }

                if (stream is SubtitleStreamConfig) {
                    if (onlySubtitles) return@forEach
                    val key = StreamKey(inputIndex, SUBTITLE, stream.streamIndex)
                    val outIndex = mapping.subtitleOutIndexMap[key] ?: stream.streamIndex

                    stream.language?.let { args += listOf("-metadata:s:s:$outIndex", "language=$it") }
                    stream.title?.let { args += listOf("-metadata:s:s:$outIndex", "title=$it") }
                    if (stream.forced) args += listOf("-disposition:s:$outIndex", "forced")
                }
            }
        }
    }

    private data class MappingInfo(
        val anyExplicitMap: Boolean,
        val videoOutIndexMap: Map<StreamKey, Int>,
        val audioOutIndexMap: Map<StreamKey, Int>,
        val subtitleOutIndexMap: Map<StreamKey, Int>
    )

    private fun compileMapping(inputs: List<InputConfig>, args: MutableList<String>): MappingInfo {
        val anyExplicitMap = inputs.any { it.allStreams().any { s -> s.map } }

        val videoMap = mutableMapOf<StreamKey, Int>()
        val audioMap = mutableMapOf<StreamKey, Int>()
        val subMap = mutableMapOf<StreamKey, Int>()

        var nextV = 0
        var nextA = 0
        var nextS = 0

        if (anyExplicitMap) {
            inputs.forEachIndexed { inputIndex, input ->
                input.allStreams().forEach { stream ->
                    if (!stream.map) return@forEach

                    when (stream.type) {
                        VIDEO -> {
                            val key = StreamKey(inputIndex, VIDEO, stream.streamIndex)
                            videoMap[key] = nextV
                            args += listOf("-map", "$inputIndex:v:${stream.streamIndex}")
                            nextV++
                        }

                        AUDIO -> {
                            val key = StreamKey(inputIndex, AUDIO, stream.streamIndex)
                            audioMap[key] = nextA
                            args += listOf("-map", "$inputIndex:a:${stream.streamIndex}")
                            nextA++
                        }

                        SUBTITLE -> {
                            val key = StreamKey(inputIndex, SUBTITLE, stream.streamIndex)
                            subMap[key] = nextS
                            args += listOf("-map", "$inputIndex:s:${stream.streamIndex}")
                            nextS++
                        }
                    }
                }
            }
        }

        return MappingInfo(anyExplicitMap, videoMap, audioMap, subMap)
    }

    private fun compileCodecs(
        inputs: List<InputConfig>,
        mapping: MappingInfo,
        args: MutableList<String>
    ) {
        inputs.forEachIndexed { inputIndex, input ->
            input.allStreams().forEach { stream ->

                val suffix: String? =
                    if (mapping.anyExplicitMap) {
                        when (stream) {
                            is VideoStreamConfig ->
                                ":${mapping.videoOutIndexMap[StreamKey(inputIndex, VIDEO, stream.streamIndex)] ?: 0}"

                            is AudioStreamConfig ->
                                ":${mapping.audioOutIndexMap[StreamKey(inputIndex, AUDIO, stream.streamIndex)] ?: 0}"

                            is SubtitleStreamConfig ->
                                ":${mapping.subtitleOutIndexMap[StreamKey(inputIndex, SUBTITLE, stream.streamIndex)] ?: 0}"

                            else -> null
                        }
                    } else null

                when (stream) {
                    is VideoStreamConfig -> {
                        val codec = stream.codec ?: return@forEach
                        args += codec.buildFfmpegArgs(suffix)
                    }

                    is AudioStreamConfig -> {
                        val codec = stream.codec ?: return@forEach
                        args += codec.buildFfmpegArgs(suffix)
                    }

                    is SubtitleStreamConfig -> {
                        val codec = stream.codec ?: return@forEach
                        args += listOf("-c:s${suffix ?: ""}", codec)
                    }
                }
            }
        }
    }
}
