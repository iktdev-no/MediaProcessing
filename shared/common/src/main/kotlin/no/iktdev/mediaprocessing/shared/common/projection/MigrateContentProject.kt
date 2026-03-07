package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.shared.common.cleanForFileSystemUse
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.common.resolveConflict
import java.io.File

open class MigrateContentProject(
    val collection: String,
    val events: List<Event>,
    val outbox: File
) {

    val useStore: File = outbox.using(collection)


    internal fun getFileName(): String? {
        val parsedInfo = events.filterIsInstance<MediaParsedInfoEvent>().lastOrNull() ?: return null
        return parsedInfo.data.parsedFileName.cleanForFileSystemUse()
    }

    internal fun isMovie(): Boolean {
        val parsedType = events
            .getInstanceOf<MediaParsedInfoEvent>()
            ?.data
            ?.mediaType

        val metadataType = events
            .filterIsInstance<MetadataSearchResultEvent>()
            .lastOrNull()
            ?.recommended
            ?.metadata
            ?.type

        // Velg første ikke-null, og sjekk om den er Movie
        return (parsedType ?: metadataType) == MediaType.Movie
    }


    fun getVideoStoreFile(): CachedToStore? {
        val encoded = events.filterIsInstance<ProcesserEncodeResultEvent>().lastOrNull()
        val cached = encoded?.data?.cachedOutputFile?.let(::File) ?: return null

        val filename = getFileName()?.let { "$it.${cached.extension}" } ?: return null
        val store = useStore?.using(filename) ?: return null

        return CachedToStore(cachedFile = cached, storeFile = store)
    }

    fun getSubtitleStoreFiles(): List<CachedToStoreLanguage>? {
        val extractedFiles = events
            .filterIsInstance<ProcesserExtractResultEvent>()
            .mapNotNull { it.data }
            .map { it.language to File(it.cachedOutputFile) }
            .groupBy({ it.first }, { it.second })

        val convertedFiles = events
            .filterIsInstance<ConvertTaskResultEvent>()
            .mapNotNull { it.data }
            .flatMap { d -> d.outputFiles.map { d.language to File(it) } }
            .groupBy({ it.first }, { it.second })

        val byLanguage = mutableMapOf<String, MutableList<File>>()
        extractedFiles.forEach { (lang, files) -> byLanguage.getOrPut(lang) { mutableListOf() }.addAll(files) }
        convertedFiles.forEach { (lang, files) -> byLanguage.getOrPut(lang) { mutableListOf() }.addAll(files) }

        val baseName = getFileName() ?: return null

        return byLanguage.flatMap { (language, files) ->
            files.mapNotNull { cached ->
                val filename = "$baseName.${cached.extension}"
                val store = useStore?.using("sub", language, filename) ?: return@mapNotNull null
                CachedToStoreLanguage(
                    cts = CachedToStore(cachedFile = cached, storeFile = store),
                    language = language
                )
            }
        }
    }

    fun getCoverStoreFiles(): CachedToStore? {
        // Finn siste cover-download
        val last = events
            .filterIsInstance<CoverDownloadResultEvent>()
            .lastOrNull() ?: return null

        val cached = last.data?.outputFile?.let(::File) ?: return null

        val store = useStore

        // Bestem base-navn for cover
        val baseName = if (isMovie()) {
            getFileName() ?: return null
        } else {
            collection.cleanForFileSystemUse()
        }

        val ext = cached.extension
        val filename = "$baseName.$ext"

        // Generer destinasjonsfil (uten conflict-resolving)
        val storeFile = store.using(filename)

        return CachedToStore(
            cachedFile = cached,
            storeFile = storeFile
        )
    }



    data class CachedToStore(val cachedFile: File, val storeFile: File)
    data class CachedToStoreLanguage(val cts: CachedToStore, val language: String)
}
