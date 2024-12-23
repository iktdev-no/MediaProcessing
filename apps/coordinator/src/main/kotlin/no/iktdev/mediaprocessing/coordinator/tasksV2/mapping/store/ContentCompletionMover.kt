package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import mu.KotlinLogging
import no.iktdev.eventi.data.dataAs
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
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
        return if (result) Pair(encodedFile.absolutePath, storeFile.absolutePath) else null
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
        val result = coverFile.moveTo(storeFile)
        return if (result) Pair(coverFile.absolutePath, storeFile.absolutePath) else null
    }


    fun getMovableSubtitles(): Map<String, List<File>> {
        val extracted =
            events.filter { it.eventType == Events.EventWorkExtractPerformed }.mapNotNull { it.dataAs<ExtractedData>() }
        val converted =
            events.filter { it.eventType == Events.EventWorkConvertPerformed }.mapNotNull { it.dataAs<ConvertedData>() }

        return extracted.groupBy { it.language }.mapValues { v -> v.value.map { File(it.outputFile) } } +
                converted.groupBy { it.language }.mapValues { v -> v.value.flatMap { it.outputFiles }.map { File(it) } }
    }

    data class MovedSubtitle(
        val language: String,
        val source: File,
        val destination: File
    )

    fun moveSubtitles(): List<MovedSubtitle>? {
        val subtitleFolder = storeFolder.using("sub")
        val moved: MutableList<MovedSubtitle> = mutableListOf()

        val subtitles = getMovableSubtitles()
        if (subtitles.isEmpty() || subtitles.values.isEmpty()) {
            return null
        }
        for ((lang, files) in subtitles) {
            val languageFolder = subtitleFolder.using(lang).also {
                if (it.notExist()) {
                    it.mkdirs()
                }
            }
            for (file in files) {
                val storeFile = languageFolder.using(file.name)
                val success = file.moveTo(storeFile)
                if (success) {
                    moved.add(MovedSubtitle(lang, file, storeFile))
                }
            }

        }
        return moved
    }


}