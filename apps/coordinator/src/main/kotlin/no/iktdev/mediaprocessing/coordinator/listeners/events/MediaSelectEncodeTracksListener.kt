package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksEncodeSelectedEvent
import org.springframework.stereotype.Component

@Component
class MediaSelectEncodeTracksListener: EventListener() {

    fun getAudioLanguagePreference(): List<String> {
        return listOf("jpn")
    }

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event as? MediaStreamParsedEvent ?: return null


        val videoTrackIndex = getVideoTrackToUse(useEvent.data.videoStream)
        val audioDefaultTrack = getAudioDefaultTrackToUse(useEvent.data.audioStream)
        val audioExtendedTrack = getAudioExtendedTrackToUse(useEvent.data.audioStream, selectedDefaultTrack = audioDefaultTrack)

        return MediaTracksEncodeSelectedEvent(
            selectedVideoTrack = videoTrackIndex,
            selectedAudioTrack = audioDefaultTrack,
            selectedAudioExtendedTrack = audioExtendedTrack
        ).derivedOf(event)
    }

    private fun getAudioExtendedTrackToUse(audioStream: List<AudioStream>, selectedDefaultTrack: Int): Int? {
        val durationFiltered = audioStream.filterOnPreferredLanguage()
            .filter { (it.duration_ts ?: 0) > 0 }
            .filter { it.channels > 2 }
            .filter { it.index != selectedDefaultTrack }
        val selected = durationFiltered.firstOrNull() ?: return null
        return audioStream.indexOf(selected)
    }

    /**
     * Select the default audio track to use for encoding.
     * If no default track is found, select the first audio track.
     * If audio track with preferred language (e.g., "nor") is not found, selects "eng" or first available.
     */
    private fun getAudioDefaultTrackToUse(audioStream: List<AudioStream>): Int {
        val durationFiltered = audioStream.filterOnPreferredLanguage()
            .filter { (it.duration_ts ?: 0) > 0 }

        val selected = durationFiltered
            .filter { it.channels == 2 }.ifEmpty { durationFiltered }
            .maxByOrNull { it.index } ?: audioStream.minByOrNull { it.index } ?: durationFiltered.firstOrNull()

        return audioStream.indexOf(selected)
    }

    /**
     * Filters audio streams based on preferred languages.
     * If no streams match the preferred languages, returns the original list.
     */
    private fun List<AudioStream>.filterOnPreferredLanguage(): List<AudioStream> {
        return this.filter { it.tags.language in getAudioLanguagePreference() }.ifEmpty { this }
    }

    private fun getVideoTrackToUse(streams: List<VideoStream>): Int {
        val selectStream = streams.filter { (it.duration_ts ?: 0) > 0 }
            .maxByOrNull { it.duration_ts ?: 0 } ?: streams.minByOrNull { it.index } ?: throw Exception("No video streams found")
        return streams.indexOf(selectStream)
    }
}