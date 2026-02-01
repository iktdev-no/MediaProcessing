package no.iktdev.mediaprocessing.processer.config

import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths
import org.springframework.stereotype.Component
import java.io.File

@Suppress("SENSELESS_COMPARISON")
@Component
class FileUtil(
    private val dirs: DirectoryProperties,
    private val mediaPaths: MediaPaths
) {
    init {
        assert(dirs.logs != null)
    }
    fun getTemporaryStoreFile(fileName: String): File =
        File(mediaPaths.cache).using(fileName)

    fun getLogDirectory(): File = File(dirs.logs)
}