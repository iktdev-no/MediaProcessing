package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import mu.KotlinLogging
import no.iktdev.eventi.data.dataAs
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.getCRC32
import no.iktdev.mediaprocessing.shared.common.moveTo
import no.iktdev.mediaprocessing.shared.common.notExist
import java.io.File

class ContentCompletionMover(val collection: String, val events: List<Event>) {
    val log = KotlinLogging.logger {}
    val storeFolder = SharedConfig.outgoingContent.using(collection)

    init {
        if (storeFolder.notExist()) {
            log.info { "Creating missing folders for path ${storeFolder.absolutePath}" }
            storeFolder.mkdirs()
        }
    }


    /**
     * @return Pair<OldPath, NewPath> or null if no file found
     */
    fun moveVideo(): Pair<String, String>? {
        val encodedFile = events.find { it.eventType == Events.EventWorkEncodePerformed }?.dataAs<EncodedData>()?.outputFile?.let {
            File(it)
        } ?: return null
        if (!encodedFile.exists()) {
            log.error { "Provided file ${encodedFile.absolutePath} does not exist at the given location" }
            return null
        }
        val storeFile = storeFolder.using(encodedFile.name)
        val result = encodedFile.moveTo(storeFile) {

        }
        return if (result) Pair(encodedFile.absolutePath, storeFile.absolutePath) else throw RuntimeException("Unable to movie file ${encodedFile.absolutePath} to ${storeFile.absolutePath}")
    }

    fun moveCover(): Pair<String, String>? {
        val coverFile = events.find { it.eventType == Events.EventWorkDownloadCoverPerformed }?.
            az<MediaCoverDownloadedEvent>()?.data?.absoluteFilePath?.let {
                File(it)
        } ?: return null

        if (coverFile.notExist()) {
            log.error { "Provided file ${coverFile.absolutePath} does not exist at the given location" }
            return null
        }
        val storeFile = storeFolder.using(coverFile.name)
        if (storeFile.exists() && storeFile.getCRC32() == coverFile.getCRC32()) {
            return Pair(coverFile.absolutePath, storeFile.absolutePath)
        }
        val result = coverFile.moveTo(storeFile)
        return if (result) Pair(coverFile.absolutePath, storeFile.absolutePath) else null
    }



    data class MovableSubtitle(
        val language: String,
        val cachedFile: File,
        val storeFileName: String
    )

    fun getMovableSubtitles(): List<MovableSubtitle> {
        val extracted =
            events.filter { it.eventType == Events.EventWorkExtractPerformed }.mapNotNull { it.dataAs<ExtractedData>() }
        val converted =
            events.filter { it.eventType == Events.EventWorkConvertPerformed }.mapNotNull { it.dataAs<ConvertedData>() }

        val items = mutableListOf<MovableSubtitle>()

        extracted.map { MovableSubtitle(
            language = it.language,
            cachedFile = File(it.outputFile),
            storeFileName = it.storeFileName
        ) }.also { items.addAll(it) }

        converted.flatMap { it.outputFiles.map { outFile ->
            MovableSubtitle(
                language = it.language,
                cachedFile = File(outFile),
                storeFileName = it.baseName
            )
        } }.also { items.addAll(it) }

        return items
    }

    data class MovedSubtitle(
        val language: String,
        val source: String,
        val destination: String
    )

    fun moveSubtitles(): List<MovedSubtitle>? {
        val subtitleFolder = storeFolder.using("sub")
        val moved: MutableList<MovedSubtitle> = mutableListOf()

        val subtitles = getMovableSubtitles()
        if (subtitles.isEmpty()) {
            return null
        }
        for (movable in subtitles) {
            val languageFolder = subtitleFolder.using(movable.language).also {
                if (it.notExist()) {
                    it.mkdirs()
                }
            }
            val storeFile = languageFolder.using("${movable.storeFileName}.${movable.cachedFile.extension}")
            val success = movable.cachedFile.moveTo(storeFile)
            if (success) {
                moved.add(MovedSubtitle(movable.language, movable.cachedFile.absolutePath, storeFile.absolutePath))
            }
        }

        return moved
    }


}