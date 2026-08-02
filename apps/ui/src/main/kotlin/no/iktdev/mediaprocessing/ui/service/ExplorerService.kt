package no.iktdev.mediaprocessing.ui.service

import no.iktdev.mediaprocessing.ui.MediaConfig
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ui.models.contract.files.FileAccessMode
import no.iktdev.mediaprocessing.ui.models.contract.files.FileAction
import no.iktdev.mediaprocessing.ui.models.contract.files.FileActionType
import no.iktdev.mediaprocessing.ui.models.contract.files.FileActions
import no.iktdev.mediaprocessing.ui.models.contract.files.Folder
import no.iktdev.mediaprocessing.ui.models.contract.files.UiFile
import no.iktdev.mediaprocessing.ui.models.contract.files.MediaAction
import no.iktdev.mediaprocessing.ui.models.contract.files.MediaActionType

import org.springframework.stereotype.Service

@Service
class ExplorerService(
    val mediaConfig: MediaConfig
) {

    fun listHome(): List<UiFile> =
        listAt(mediaConfig.inbox)

    fun listAt(path: String): List<UiFile> {
        val dir = IFile(path)

        if (!dir.exists() || !dir.isDirectory()) {
            return emptyList()
        }

        return dir.listFiles()
            ?.map { file ->
                file.toFileInfo()
            }
            ?: emptyList()
    }


    fun pathToFile(path: String): UiFile? {
        val file = IFile(path)
        if (file.notExist())
            return null
        return file.toFileInfo()
    }

    fun IFile.toFileInfo(): UiFile {
        val file = this
        val access = determineAccessMode(file)
        return if (file.isDirectory()) {
            Folder(
                name = file.name,
                uri = file.absolutePath,
                created = file.lastModified(),
                actions = FileActions(
                    mediaActions = emptyList(),
                    fileActions = getFileActions(file, access),
                ),
                accessMode = access
            )
        } else {
            no.iktdev.mediaprocessing.ui.models.contract.files.File(
                name = file.name,
                uri = file.absolutePath,
                created = file.lastModified(),
                extension = file.extension(),
                actions = FileActions(
                    mediaActions = getMediaActionsForFile(file),
                    fileActions = getFileActions(file, access),
                ),
                size = file.length(),
                accessMode = access
            )
        }
    }

    fun getFileActions(file: IFile, accessMode: FileAccessMode): List<FileAction> {
        val actions = mutableListOf<FileAction>()
        if (accessMode == FileAccessMode.NO_ACCESS) return actions

        if (file.isDirectory()) {
            actions.add(FileAction(id = FileActionType.Open, requiresConfirmation = false))
        }
        if (accessMode == FileAccessMode.READ_WRITE) {
            actions.add(FileAction(id = FileActionType.Delete, requiresConfirmation = true))
        }

        return actions
    }


    fun getMediaActionsForFile(file: IFile): List<MediaAction> {
        val ext = file.extension().lowercase()

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
                MediaAction(MediaActionType.ExtractSubtitles),
                MediaAction(MediaActionType.MetadataSearch),
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

    fun determineAccessMode(file: IFile): FileAccessMode {
        if (!file.exists()) {
            return FileAccessMode.NO_ACCESS
        }

        // If we cannot read the file at all → no access
        if (!file.canRead()) {
            return FileAccessMode.NO_ACCESS
        }

        // For deletion, write access must exist on the parent directory
        val parent = file.parentFile
        val parentWritable = parent?.canWrite() ?: false

        // File itself must also be writable for RW
        val fileWritable = file.canWrite()

        return if (fileWritable && parentWritable) {
            FileAccessMode.READ_WRITE
        } else {
            FileAccessMode.READ_ONLY
        }
    }


}
