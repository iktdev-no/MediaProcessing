package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.shared.common.cleanForFileSystem
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.common.resolveConflict
import java.io.File

open class MigrateContentProject(
    val events: List<Event>,
    val storageArea: File
) {

    val useStore: File? = getDesiredStoreFolder()

    open fun getFoldersInStore(): List<String> {
        return storageArea
            .listFiles { file -> file.isDirectory }
            ?.map { it.name }
            ?: emptyList()
    }

    internal fun getFileName(): String? {
        val parsedInfo = events.filterIsInstance<MediaParsedInfoEvent>().lastOrNull() ?: return null
        return parsedInfo.data.parsedFileName.cleanForFileSystem()
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



    internal fun getDesiredStoreFolder(): File? {
        val desiredCollection = getDesiredCollection()?.cleanForFileSystem() ?: return null
        val assuredStore = storageArea.using(desiredCollection)

        val existingCollectionNames = getFoldersInStore()
        if (existingCollectionNames.isEmpty()) {
            return assuredStore
        }

        val titles = getMetadataTitles().map { it.cleanForFileSystem() }

        val matchedExisting = titles
            .firstOrNull { it in existingCollectionNames }
            ?.let { storageArea.using(it) }

        return matchedExisting ?: assuredStore
    }

    internal fun getMetadataTitles(): List<String> {
        val metadataEvent = events
            .filterIsInstance<MetadataSearchResultEvent>()
            .lastOrNull()
            ?.recommended
            ?.metadata
            ?: return emptyList()

        return metadataEvent.alternateTitles + metadataEvent.title
    }

    internal fun getDesiredCollection(): String? {
        return events
            .filterIsInstance<MediaParsedInfoEvent>()
            .lastOrNull()
            ?.data
            ?.parsedCollection
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

    fun getCoverStoreFiles(): List<CachedToStore>? {
        val downloaded = events
            .filterIsInstance<CoverDownloadResultEvent>()
            .mapNotNull { e ->
                val file = e.data?.outputFile?.let(::File) ?: return@mapNotNull null
                e to file
            }
        if (downloaded.isEmpty()) return null
        val store = useStore ?: return null



        val storeCoverFileName = if (isMovie()) {
            getFileName()
        } else {
            getDesiredCollection()?.cleanForFileSystem() ?: return null

        }

        val multiple = downloaded.size > 1

        return downloaded.mapNotNull { (event, cached) ->
            val ext = cached.extension
            val source = event.data?.source ?: "unknown"

            val filename = if (multiple || store.using("$storeCoverFileName.$ext").exists()) {
                "$storeCoverFileName-$source.$ext"
            } else {
                "$storeCoverFileName.$ext"
            }

            val storeFile = store.using(filename).resolveConflict()

            CachedToStore(cachedFile = cached, storeFile = storeFile)
        }
    }

    data class CachedToStore(val cachedFile: File, val storeFile: File)
    data class CachedToStoreLanguage(val cts: CachedToStore, val language: String)
}
