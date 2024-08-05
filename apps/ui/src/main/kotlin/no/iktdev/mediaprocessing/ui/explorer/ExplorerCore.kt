package no.iktdev.mediaprocessing.ui.explorer

import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.ui.dto.explore.ExplorerAttributes
import no.iktdev.mediaprocessing.ui.dto.explore.ExplorerCursor
import no.iktdev.mediaprocessing.ui.dto.explore.ExplorerItem
import no.iktdev.mediaprocessing.ui.dto.explore.ExplorerItemType
import java.io.File
import java.io.FileFilter
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributeView

class ExplorerCore {

    fun getCursor(path: String): ExplorerCursor? {
        val file = File(path)
        if (!file.exists() || file.isFile) {
            return null
        }
        return ExplorerCursor(
            name = file.name,
            path = file.absolutePath,
            items = getFiles(file) + getFolders(file),
        )
    }

    fun fromFile(file: File): ExplorerItem? {
        if (!file.exists())
            return null
        val attr = getAttr(file)
        return ExplorerItem(
            path = file.absolutePath,
            name = file.nameWithoutExtension,
            extension = file.extension,
            created = attr.created,
            type = ExplorerItemType.FILE
        )
    }

    private fun getFiles(inDirectory: File): List<ExplorerItem> {
        return inDirectory.listFiles(FileFilter { it.isFile })?.map {
            val attr = getAttr(it)
            ExplorerItem(
                path = it.absolutePath,
                name = it.nameWithoutExtension,
                extension = it.extension,
                created = attr.created,
                type = ExplorerItemType.FILE
            )
        } ?: emptyList()
    }

    private fun getFolders(inDirectory: File): List<ExplorerItem> {
        return inDirectory.listFiles(FileFilter { it.isDirectory })?.map {
            val attr = getAttr(it)
            ExplorerItem(
                name = it.name,
                path = it.absolutePath,
                created = attr.created,
                type = ExplorerItemType.FOLDER
            )
        } ?: emptyList()
    }

    private fun getAttr(item: File): ExplorerAttributes {
        val attrView = Files.getFileAttributeView(item.toPath(), BasicFileAttributeView::class.java).readAttributes()
        return ExplorerAttributes(
            created = attrView.creationTime().toMillis()
        )
    }

    fun getHomeCursor(): ExplorerCursor? {
        return getCursor(SharedConfig.incomingContent.absolutePath)
    }

}