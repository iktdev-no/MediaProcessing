package no.iktdev.mediaprocessing.processer.config

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths
import org.springframework.stereotype.Component

@Suppress("SENSELESS_COMPARISON")
@Component
class FileUtil(
    private val dirs: DirectoryProperties,
    private val mediaPaths: MediaPaths
) {
    fun getTemporaryStoreFile(fileName: String): IFile =
        getTemporaryStoreFolder(fileName).using(fileName)

    fun getTemporaryStoreFolder(fileName: String): IFile {
        val temporaryStore = IFile(mediaPaths.intermediate).using(IFile(fileName).nameWithoutExtension)
        return temporaryStore
    }

    fun getLogDirectory(): IFile {
        return IFile(dirs.logs)
    }
}