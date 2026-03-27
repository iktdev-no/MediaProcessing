package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.cleanForFileSystemUse
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.isOnly
import no.iktdev.mediaprocessing.shared.common.getInstanceOf

class CollectionProjection(
    val history: List<Event>,
    val outbox: IFile
) {

    fun getCollection(): String {
        val started = history.getInstanceOf<StartProcessingEvent>() ?: throw IllegalStateException("No start processing events found")
        if (started.data.operation.isOnly(OperationType.ConvertSubtitles)) {
            return getCollectionAltFlowConvert(started)
        }

        val collectionCandidates = mutableListOf<String>()
        history.getInstanceOf<MediaParsedInfoEvent>()?.data?.parsedCollection?.let { collection ->
            collectionCandidates.add(collection)
        }
        history.getInstanceOf<MetadataSearchResultEvent>()?.recommended?.metadata?.let { metadata ->
            collectionCandidates.add(metadata.title)
            collectionCandidates.addAll(metadata.alternateTitles)
        }

        if (collectionCandidates.isEmpty()) {
            throw NoSuchElementException("No collection candidates found")
        }

        val stores = outbox
            .listFiles().filter { it.isDirectory() }
            .map { it.name }

        // Finn første kandidat som matcher en eksisterende mappe
        for (candidate in collectionCandidates) {
            val match = stores.firstOrNull { store ->
                compareNames(candidate, store)
            }
            if (match != null) {
                return match // A: bruk eksisterende mappe
            }
        }
        return collectionCandidates.first().cleanForFileSystemUse()
    }

    fun getCollectionAltFlowConvert(started: StartProcessingEvent): String {
        val useFile = started.data.fileUri.let { IFile(it) }
        val collection = useFile.parentFile.parentFile.parentFile.name // "language->sub->collection"
        return collection
    }



    fun compareNames(a: String, b: String): Boolean {
        return normalizeForComparison(a) == normalizeForComparison(b)
    }

    fun normalizeForComparison(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[!?,.:;'\"`()\\[\\]]"), " ")
            .replace("-", " ")
            .replace("_", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

}