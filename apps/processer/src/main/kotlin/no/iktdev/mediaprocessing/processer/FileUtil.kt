package no.iktdev.mediaprocessing.processer

import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.processer.config.DirectoryProperties
import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths
import org.springframework.stereotype.Component
import java.io.File

@Component
class FileUtil(
    private val dirs: DirectoryProperties,
    private val mediaPaths: MediaPaths
) {
    fun getTemporaryStoreFile(fileName: String): File =
        File(mediaPaths.cache).using(fileName)

    fun getLogDirectory(): File = File(dirs.logs)
}
