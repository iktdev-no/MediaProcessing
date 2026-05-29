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
    fun getTemporaryStoreFolder(folderName: String): IFile =
        IFile(mediaPaths.intermediate).using(folderName)

    fun getLogDirectory(): IFile {
        return IFile(dirs.logs)
    }
}