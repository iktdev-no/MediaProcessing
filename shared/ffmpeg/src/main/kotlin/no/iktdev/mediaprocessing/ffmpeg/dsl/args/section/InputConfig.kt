package no.iktdev.mediaprocessing.ffmpeg.dsl.args.section

class InputConfig(
    val path: String
) {
    val videoStreams: MutableList<VideoStreamConfig> = mutableListOf()
    val audioStreams: MutableList<AudioStreamConfig> = mutableListOf()
    val subtitleStreams: MutableList<SubtitleStreamConfig> = mutableListOf()

    fun allStreams(): List<StreamConfig> {
        return videoStreams + audioStreams + subtitleStreams
    }

    fun video(index: Int, block: VideoStreamConfig.() -> Unit) {
        videoStreams += VideoStreamConfig(index).apply(block)
    }

    fun audio(index: Int, block: AudioStreamConfig.() -> Unit) {
        audioStreams += AudioStreamConfig(index).apply(block)
    }

    fun subtitle(index: Int, block: SubtitleStreamConfig.() -> Unit) {
        subtitleStreams += SubtitleStreamConfig(index).apply(block)
    }

}

class InputSection {

    private val inputs: MutableList<InputConfig> = mutableListOf()
    fun files(): List<InputConfig> {
        return inputs.toList()
    }

    var concatInput: ConcatInputConfig? = null
        private set

    fun file(path: String, block: InputConfig.() -> Unit) {
        concatInput = null
        inputs += InputConfig(path).apply(block)
    }

    fun concat(listFile: String, block: ConcatInputConfig.() -> Unit) {
        inputs.clear()
        ensureEmpty()
        concatInput = ConcatInputConfig(listFile).apply(block)
    }



    private fun ensureEmpty() {
        if (inputs.isNotEmpty()) {
            error("Concat input cannot be combined with other inputs")
        }
    }
}


class ConcatInputConfig(
    val listFile: String
) {}

