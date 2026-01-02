package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.resolveConflict
import java.io.File

open class MigrateContentProject(val events: List<Event>, val storageArea: File) {
    val useStore: File? = getDesiredStoreFolder()

    open fun getFoldersInStore(): List<String> {
        return storageArea.listFiles { file -> file.isDirectory }?.map { it.name }?.toList() ?: emptyList()
    }

    internal fun getFileName(): String? {
        val parsedInfo = events.filterIsInstance<MediaParsedInfoEvent>().lastOrNull() ?: return null
        return parsedInfo.data.parsedFileName
    }

    internal fun getDesiredStoreFolder(): File? {
        val assuredCollection = getDesiredCollection() ?: return null
        val assuredStore = storageArea.using(assuredCollection)
        val existingCollectionNames = getFoldersInStore().ifEmpty {
            return assuredStore
        }
        val titles = getMetadataTitles()
        val existingCollection = titles.filter { it in existingCollectionNames }.firstOrNull()?.let {
            File(it)
        } ?: assuredStore
        return existingCollection
    }

    internal fun getMetadataTitles(): List<String> {
        val metadataEvent = events.filterIsInstance<MetadataSearchResultEvent>().lastOrNull()?.recommended?.data
            ?: return emptyList()
        return (metadataEvent.alternateTitles + listOf(metadataEvent.title))
    }

    internal fun getDesiredCollection(): String? {
        val metadataEvent = events.filterIsInstance<MediaParsedInfoEvent>().lastOrNull()?.data
        return metadataEvent?.parsedCollection
    }


    fun getVideoStoreFile(): CachedToStore? {
        val encoded = events.filterIsInstance<ProcesserEncodeResultEvent>().lastOrNull()
        val cached = encoded?.data?.cachedOutputFile?.let { File(it) } ?: return null
        val useFilename = getFileName()?.let { "$it.${cached.extension}" } ?: return null
        val store = useStore?.using(useFilename) ?: return null
        return CachedToStore(
            cachedFile = cached,
            storeFile = store
        )
    }

    fun getSubtitleStoreFiles(): List<CachedToStoreLanguage>? {
        val extractedFiles: Map<String, List<File>> = events
            .filterIsInstance<ProcesserExtractResultEvent>()
            .mapNotNull { it.data }
            .map { data -> data.language to File(data.cachedOutputFile) }
            .groupBy({ it.first }, { it.second })

        val convertedFilesGrouped: Map<String, List<File>> = events
            .filterIsInstance<ConvertTaskResultEvent>()
            .mapNotNull { it.data }
            .map { x -> x.outputFiles.map { x.language to File(it) } }
            .flatten()
            .groupBy({ it.first }, { it.second })


        val byLanguage = mutableMapOf<String, MutableList<File>>()
        extractedFiles.forEach { (lang, files) ->
            byLanguage.getOrPut(lang) { mutableListOf() }.addAll(files)
        }
        convertedFilesGrouped.forEach { (lang, files) ->
            byLanguage.getOrPut(lang) { mutableListOf() }.addAll(files)
        }

        val useFilename = getFileName() ?: return null

        return byLanguage.flatMap { (language, files) ->
            files.mapNotNull { cached ->
                val useFilename = "$useFilename.${cached.extension}"
                val store = useStore?.using(language, useFilename) ?: return@mapNotNull null
                CachedToStoreLanguage(
                    CachedToStore(
                        cachedFile = cached,
                        storeFile = store
                    ),
                    language = language
                )
            }
        }
    }

    fun getCoverStoreFiles(): List<CachedToStore>? {
        val downloaded = events.filterIsInstance<CoverDownloadResultEvent>()
            .mapNotNull { event ->
                val file = event.data?.outputFile?.let(::File) ?: return@mapNotNull null
                event to file
            }

        val baseName = getDesiredCollection() ?: return null
        val store = useStore ?: return null

        val multiple = downloaded.size > 1

        return downloaded.mapNotNull { (event, cached) ->
            val ext = cached.extension
            val source = event.data?.source ?: "unknown"

            // Bestem om vi skal bruke source i navnet
            val useSource = multiple || store.using("$baseName.$ext").exists()

            val filename = if (useSource) {
                "$baseName-$source.$ext"
            } else {
                "$baseName.$ext"
            }

            val storeFile = store.using(filename).resolveConflict()

            CachedToStore(
                cachedFile = cached,
                storeFile = storeFile
            )
        }
    }


    data class CachedToStore(
        val cachedFile: File,
        val storeFile: File
    )

    data class CachedToStoreLanguage(
        val cts: CachedToStore,
        val language: String
    )
}