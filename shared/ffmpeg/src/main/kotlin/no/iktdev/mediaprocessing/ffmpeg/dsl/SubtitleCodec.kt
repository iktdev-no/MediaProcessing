package no.iktdev.mediaprocessing.ffmpeg.dsl

import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream

sealed class SubtitleCodec(val codec: String) {
    class Srt(): SubtitleCodec("srt") {
        override fun getExtension(): String = "srt"
    }

    class Vtt(): SubtitleCodec("vtt") {
        override fun getExtension(): String = "vtt"
    }
    class Ass(): SubtitleCodec("ass") {
        override fun getExtension(): String = "ass"
    }
    class Smi(): SubtitleCodec("smi") {
        override fun getExtension(): String = "smi"
    }

    open fun buildFfmpegArgs(stream: SubtitleStream): List<String> {
        return mutableListOf("-c:s", "copy")
    }
    abstract fun getExtension(): String

    companion object {
        /**
         * @return null if not supported
         */
        fun getCodec(codecName: String): SubtitleCodec? {
            return when (codecName) {
                // ffmpeg bruker "subrip" for SRT
                "srt", "subrip" -> Srt()
                // webvtt
                "vtt", "webvtt" -> Vtt()
                // ass/ssa
                "ass", "ssa" -> Ass()
                // smi/sami
                "smi", "sami" -> Smi()
                else -> null
            }
        }
    }
}

