package no.iktdev.mediaprocessing.ffmpeg.dsl.args.section

class InputConfig(
    path: String
): BaseInputConfig(path = path) {
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

    internal val inputs: MutableList<BaseInputConfig> = mutableListOf()

    fun file(path: String, block: InputConfig.() -> Unit) {
        ensureNotConcat()
        inputs += InputConfig(path).apply(block)
    }

    fun concat(listFile: String, block: ConcatInputConfig.() -> Unit) {
        inputs.clear()
        ensureEmpty()
        inputs += ConcatInputConfig(listFile).apply(block)
    }

    fun all(): List<BaseInputConfig> = inputs.toList()

    private fun ensureNotConcat() {
        if (inputs.any { it is ConcatInputConfig }) {
            error("Cannot mix concat input with normal file inputs")
        }
    }

    private fun ensureEmpty() {
        if (inputs.isNotEmpty()) {
            error("Concat input cannot be combined with other inputs")
        }
    }
}


class ConcatInputConfig(
    val listFile: String
): BaseInputConfig(path = listFile) {}

open class BaseInputConfig(val path: String) {}