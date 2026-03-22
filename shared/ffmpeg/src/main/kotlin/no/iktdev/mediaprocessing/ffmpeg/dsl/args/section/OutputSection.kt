package no.iktdev.mediaprocessing.ffmpeg.dsl.args.section

class OutputSection(val path: String) {
    var overwrite: Boolean = false
    var progress: Boolean = false
    var useWorkFile: Boolean = true
    val workFile: String
        get() {
            val dotIndex = path.lastIndexOf('.')
            return if (dotIndex != -1) {
                path.substring(0, dotIndex) + ".work" + path.substring(dotIndex)
            } else {
                "$path.work"
            }
        }

    fun resolvedName(): String = if (useWorkFile) workFile else path

}
