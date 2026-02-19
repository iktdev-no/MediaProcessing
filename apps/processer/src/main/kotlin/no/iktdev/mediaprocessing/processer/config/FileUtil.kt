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
    fun getTemporaryStoreFile(fileName: String): File =
        getTemporaryStoreFolder(fileName).using(fileName)

    fun getTemporaryStoreFolder(fileName: String): File {
        val temporaryStore = File(mediaPaths.intermediate).using(File(fileName).nameWithoutExtension)
        return temporaryStore
    }

    fun getLogDirectory(): File {
        return File(dirs.logs)
    }
}