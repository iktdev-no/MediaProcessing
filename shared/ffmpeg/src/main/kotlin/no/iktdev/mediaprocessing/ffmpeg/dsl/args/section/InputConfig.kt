package no.iktdev.mediaprocessing.ffmpeg.dsl.args.section

class InputConfig(
    val path: String
) {
    val streams: MutableList<StreamConfig> = mutableListOf()

    fun video(index: Int, block: VideoStreamConfig.() -> Unit) {
        streams += VideoStreamConfig(index).apply(block)
    }

    fun audio(index: Int, block: AudioStreamConfig.() -> Unit) {
        streams += AudioStreamConfig(index).apply(block)
    }

    fun subtitle(index: Int, block: SubtitleStreamConfig.() -> Unit) {
        streams += SubtitleStreamConfig(index).apply(block)
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

