package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.ListenerOrder
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.events.SoftDispatchException
import no.iktdev.eventi.models.Event
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.coordinator.parse.MovieParsing
import no.iktdev.mediaprocessing.coordinator.parse.SerieParsing
import no.iktdev.mediaprocessing.coordinator.parse.evaluateMediaType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.isOnly
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import org.springframework.stereotype.Component

@ListenerOrder(2)
@Component
class MediaParsedInfoListener : EventListener() {
    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val started = event.requireQualifiedEntry<StartProcessingEvent>()
        if (started.data.operation.isOnly(OperationType.ConvertSubtitles)) {
            return null
        }
        val file = IFile(started.data.fileUri)
        val mediaType = file.evaluateMediaType()

        val parser = when (mediaType) {
            MediaType.Movie -> MovieParsing()
            MediaType.Serie -> SerieParsing()
            else -> throw SoftDispatchException.ForcedListenerEjectionException("Evaluated media type is not supported!: $mediaType", event::class.java)
        }

        val fileName = parser.extractFilename(file)
        val collection = parser.extractCollection(file)
        val searchTitles = parser.extractTitles(file)
        val episodeInfo = parser.extractEpisodeInfo(file)

        return MediaParsedInfoEvent(
            MediaParsedInfoEvent.ParsedData(
                parsedFileName = fileName,
                parsedCollection = collection,
                parsedSearchTitles = searchTitles,
                mediaType = mediaType,
                episodeInfo = episodeInfo,
            )
        ).derivedOf(event)
    }



}