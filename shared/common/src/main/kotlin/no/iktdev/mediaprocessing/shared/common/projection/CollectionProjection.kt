package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.cleanForFileSystemUse
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.DeterminedCollectionTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.isOnly
import no.iktdev.mediaprocessing.shared.common.getInstanceOf

class CollectionProjection(
    val history: List<Event>
) {

    fun getCollection(): String {
        val started = history.getInstanceOf<StartProcessingEvent>()
            ?: throw IllegalStateException("No start processing events found")

        if (started.data.operation.isOnly(OperationType.ConvertSubtitles)) {
            return getCollectionAltFlowConvert(started)
        }

        // 1. Sjekk om kolleksjonen allerede har blitt bestemt og lagret
        val determinedEvent = history.filterIsInstance<DeterminedCollectionTaskResultEvent>()
            .lastOrNull { it.status == TaskStatus.Completed && !it.collection.isNullOrBlank() }

        if (determinedEvent?.collection != null) {
            return determinedEvent.collection
        }

        val collectionCandidates = mutableListOf<String>()

        // 2. Metadata (og alternative titler) skal ha høyere prioritet enn parseren!
        history.getInstanceOf<MetadataSearchResultEvent>()?.recommended?.metadata?.let { metadata ->
            collectionCandidates.add(metadata.title)
            collectionCandidates.addAll(metadata.alternateTitles)
        }

        // 3. Parser-informasjon fungerer som fallback hvis metadata mangler
        history.getInstanceOf<MediaParsedInfoEvent>()?.data?.parsedCollection?.let { collection ->
            collectionCandidates.add(collection)
        }

        if (collectionCandidates.isEmpty()) {
            throw NoSuchElementException("No collection candidates found")
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