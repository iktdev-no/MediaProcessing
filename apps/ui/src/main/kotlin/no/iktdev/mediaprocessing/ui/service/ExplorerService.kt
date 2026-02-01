package no.iktdev.mediaprocessing.ui.service

import no.iktdev.mediaprocessing.shared.common.notExist
import no.iktdev.mediaprocessing.ui.MediaConfig
import no.iktdev.mediaprocessing.ui.dto.file.*
import org.springframework.stereotype.Service
import java.io.File

@Service
class ExplorerService(
    val mediaConfig: MediaConfig
) {

    fun listHome(): List<IFile> =
        listAt(mediaConfig.incoming)

    fun listAt(path: String): List<IFile> {
        val dir = File(path)

        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }

        return dir.listFiles()
            ?.map { file ->
                file.toFileInfo()
            }
            ?: emptyList()
    }


    fun pathToFile(path: String): IFile? {
        val file = File(path)
        if (file.notExist())
            return null
        return file.toFileInfo()
    }

    fun File.toFileInfo(): IFile {
        val file = this
        return if (file.isDirectory) {
            FolderItem(
                name = file.name,
                uri = file.absolutePath,
                created = file.lastModified()
            )
        } else {
            FileItem(
                name = file.name,
                uri = file.absolutePath,
                created = file.lastModified(),
                extension = file.extension,
                actions = FileActions(mediaActions = getMediaActionsForFile(file), fileActions = listOf(
                    FileAction(id = FileActionType.Delete, requiresConfirmation = true)
                ))
            )
        }
    }


    fun getMediaActionsForFile(file: File): List<MediaAction> {
        val ext = file.extension.lowercase()

        // 1. Subtitle files → Convert
        val subtitleExt = setOf("srt", "ass", "smi", "vtt")
        if (ext in subtitleExt) {
            return listOf(MediaAction(MediaActionType.ConvertSubtitle))
        }

        // 2. Video containers that support embedded subtitles
        val subtitleVideoContainers = setOf("mkv", "mp4", "mov", "webm")
        if (ext in subtitleVideoContainers) {
            return listOf(
                MediaAction(MediaActionType.All),
                MediaAction(MediaActionType.Encode),
                MediaAction(MediaActionType.ExtractSubtitles)
            )
        }

        // 3. Video containers that do NOT support subtitles
        val nonSubtitleVideoContainers = setOf("avi", "mpeg", "mpg", "ts", "m2ts", "wmv", "flv")
        if (ext in nonSubtitleVideoContainers) {
            return listOf(MediaAction(MediaActionType.Encode))
        }

        // 4. Everything else → no media actions
        return emptyList()
    }


}
