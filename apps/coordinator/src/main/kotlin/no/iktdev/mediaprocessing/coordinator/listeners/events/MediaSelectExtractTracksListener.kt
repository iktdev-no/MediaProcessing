package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksDetermineSubtitleTypeEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksExtractSelectedEvent
import no.iktdev.mediaprocessing.shared.common.model.SubtitleType
import org.springframework.stereotype.Component

@Component
class MediaSelectExtractTracksListener: EventListener() {

    fun limitToLanguages(): Set<String> {
        return emptySet()
    }

    fun useTypes(): Set<SubtitleType> {
        return setOf(SubtitleType.Dialogue)
    }

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event as? MediaTracksDetermineSubtitleTypeEvent ?: return null

        val filtered = useEvent.subtitleTrackItems
            .filter { it.type in useTypes() }
            .map { it.stream }
            .filterOnPreferredLanguage()

        return MediaTracksExtractSelectedEvent(
            selectedSubtitleTracks = filtered.map { it.index }
        )
    }


    private fun List<SubtitleStream>.filterOnPreferredLanguage(): List<SubtitleStream> {
        val languages = limitToLanguages()
        if (languages.isEmpty()) return this
        return this.filter { it.tags.language != null }.filter { languages.contains(it.tags.language) }
    }
}