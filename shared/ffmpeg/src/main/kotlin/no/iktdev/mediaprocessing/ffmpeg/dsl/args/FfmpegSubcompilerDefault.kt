package no.iktdev.mediaprocessing.ffmpeg.dsl.args

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.*
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.StreamType.*

class FfmpegSubcompilerDefault(
    private val inputs: InputSection,
    private val output: OutputSection?,
    private val segment: SegmentSection? = null,
    private val outStore: IFile? = null
) {

    fun compile(): List<String> {
        val inputFiles = inputs.files()
        require(inputFiles.isNotEmpty()) { "At least one input is required" }

        val args = mutableListOf<String>()
        val out = requireNotNull(output)

        if (out.overwrite) args += "-y"
        args += listOf("-nostdin", "-nostats", "-hide_banner")
        args += "-dn"

        segment?.start?.let { args += listOf("-ss", it.toString()) }
        segment?.duration?.let { args += listOf("-t", it.toString()) }

        inputFiles.forEach { input -> args += listOf("-i", input.path) }

        val mappingInfo = compileMapping(inputFiles, args)

        compileMetadata(inputFiles, mappingInfo, args)
        compileCodecs(inputFiles, mappingInfo, args)
        compileDisposition(inputFiles, mappingInfo, args)   // <-- NYTT OG KRITISK

        return args
    }

    // ---------------------------------------------------------
    // METADATA (language/title/handler_name)
    // ---------------------------------------------------------
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

                    val lang = stream.language ?: "Audio"
                    val ch = stream.codec?.channels ?: stream.codec?.channels ?: -1
                    val chLabel = when (ch) {
                        1 -> "1ch"
                        2 -> "2ch"
                        6 -> "6ch"
                        else -> "${ch}ch"
                    }

                    args += listOf("-metadata:s:a:$outIndex", "handler_name=$lang $chLabel")
                }

                if (stream is SubtitleStreamConfig) {
                    if (onlySubtitles) return@forEach
                    val key = StreamKey(inputIndex, SUBTITLE, stream.streamIndex)
                    val outIndex = mapping.subtitleOutIndexMap[key] ?: stream.streamIndex

                    stream.language?.let { args += listOf("-metadata:s:s:$outIndex", "language=$it") }
                    stream.title?.let { args += listOf("-metadata:s:s:$outIndex", "title=$it") }
                }
            }
        }
    }

    // ---------------------------------------------------------
    // DISPOSITION (MÅ komme etter codecs)
    // ---------------------------------------------------------
    private fun compileDisposition(
        inputs: List<InputConfig>,
        mapping: MappingInfo,
        args: MutableList<String>
    ) {
        val audioStreams = inputs.flatMapIndexed { inputIndex, input ->
            input.allStreams().filterIsInstance<AudioStreamConfig>().map { stream ->
                Triple(inputIndex, stream.streamIndex, stream)
            }
        }

        if (audioStreams.isEmpty()) return

        // --- ALWAYS ensure exactly ONE default ---
        val defaultStream = audioStreams.find { (_, _, s) -> s.default } ?: audioStreams.first()
        val (defInputIdx, defStreamIdx, _) = defaultStream
        val defaultOutIndex = mapping.audioOutIndexMap[StreamKey(defInputIdx, AUDIO, defStreamIdx)]!!

        audioStreams.forEach { (iIdx, sIdx, stream) ->
            val outIdx = mapping.audioOutIndexMap[StreamKey(iIdx, AUDIO, sIdx)]!!

            // --- DEFAULT (exactly one) ---
            if (outIdx == defaultOutIndex) {
                args += listOf("-disposition:a:$outIdx", "default")
            }

            // --- OTHER DISPOSITIONS ---
            if (stream.forced)
                args += listOf("-disposition:a:$outIdx", "forced")

            if (stream.commentary)
                args += listOf("-disposition:a:$outIdx", "commentary")

            if (stream.descriptive)
                args += listOf("-disposition:a:$outIdx", "descriptions")

            if (stream.hearingImpaired)
                args += listOf("-disposition:a:$outIdx", "hearing_impaired")

            if (stream.original)
                args += listOf("-disposition:a:$outIdx", "original")

            // --- If this stream has NO flags at all, clear disposition ---
            if (
                outIdx != defaultOutIndex &&
                !stream.forced &&
                !stream.commentary &&
                !stream.descriptive &&
                !stream.hearingImpaired &&
                !stream.original
            ) {
                args += listOf("-disposition:a:$outIdx", "0")
            }
        }
    }



    // ---------------------------------------------------------
    // RESTEN ER IDENTISK
    // ---------------------------------------------------------
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
