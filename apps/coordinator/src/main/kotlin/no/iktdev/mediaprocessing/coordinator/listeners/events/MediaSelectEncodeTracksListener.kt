package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksEncodeSelectedEvent
import org.springframework.stereotype.Component

@Component
class MediaSelectEncodeTracksListener(
    private val preference: Preference
) : EventListener() {

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event as? MediaStreamParsedEvent ?: return null

        val videoTrackIndex = getVideoTrackToUse(useEvent.data.videoStream)
        val audioDefaultTrack = getAudioDefaultTrackToUse(useEvent.data.audioStream)
        val audioExtendedTrack = getAudioExtendedTrackToUse(
            useEvent.data.audioStream,
            selectedDefaultTrack = audioDefaultTrack
        )

        return MediaTracksEncodeSelectedEvent(
            selectedVideoTrack = videoTrackIndex,
            selectedAudioTrack = audioDefaultTrack,
            selectedAudioExtendedTrack = audioExtendedTrack
        ).derivedOf(event)
    }

    // ------------------------------------------------------------
    // AUDIO SELECTION
    // ------------------------------------------------------------



    private fun getAudioDefaultTrackToUse(audioStreams: List<AudioStream>): Int {
        val pref = preference.getLanguagePreference()

        val selected = selectBestAudioStream(
            streams = audioStreams,
            preferredLanguages = pref.preferredAudio,
            preferOriginal = pref.preferOriginal,
            avoidDub = pref.avoidDub,
            mode = AudioSelectMode.DEFAULT
        ) ?: audioStreams.firstOrNull()

        return audioStreams.indexOf(selected)
    }

    private fun getAudioExtendedTrackToUse(
        audioStreams: List<AudioStream>,
        selectedDefaultTrack: Int
    ): Int? {
        val pref = preference.getLanguagePreference()

        val candidates = audioStreams
            .filter { it.index != selectedDefaultTrack }
            .filter { (it.duration_ts ?: 0) > 0 }
            .filter { it.channels > 2 }

        val selected = selectBestAudioStream(
            streams = candidates,
            preferredLanguages = pref.preferredAudio,
            preferOriginal = pref.preferOriginal,
            avoidDub = pref.avoidDub,
            mode = AudioSelectMode.EXTENDED
        ) ?: return null

        return audioStreams.indexOf(selected)
    }


    // ------------------------------------------------------------
    // CORE AUDIO SELECTION LOGIC
    // ------------------------------------------------------------

    private enum class AudioSelectMode { DEFAULT, EXTENDED }

    private fun selectBestAudioStream(
        streams: List<AudioStream>,
        preferredLanguages: List<String>,
        preferOriginal: Boolean,
        avoidDub: Boolean,
        mode: AudioSelectMode
    ): AudioStream? {
        if (streams.isEmpty()) return null

        // 1. Originalspråk
        if (preferOriginal) {
            val originals = streams.filter { it.disposition.original == 1 }
            if (originals.isNotEmpty()) {
                return when (mode) {
                    AudioSelectMode.DEFAULT -> originals.minByOrNull { it.channels }
                    AudioSelectMode.EXTENDED -> originals.maxByOrNull { it.channels }
                }
            }
        }

        // 2. Filtrer bort dub
        val filtered = if (avoidDub) {
            streams.filter { it.disposition.dub != 1 }
        } else streams

        // 3. Foretrukne språk
        for (lang in preferredLanguages) {
            val match = filtered.filter {
                it.tags.language?.equals(lang, ignoreCase = true) == true
            }
            if (match.isNotEmpty()) {
                return when (mode) {
                    AudioSelectMode.DEFAULT -> match.minByOrNull { it.channels }
                    AudioSelectMode.EXTENDED -> match.maxByOrNull { it.channels }
                }
            }
        }

        // 4. Default-flagget
        val default = filtered.firstOrNull { it.disposition.default == 1 }
        if (default != null) return default

        // 5. Fallback
        return filtered.firstOrNull()
    }


    // ------------------------------------------------------------
    // QUALITY SCORE (no bitrate)
    // ------------------------------------------------------------

    private fun qualityScore(s: AudioStream): Int {
        val channelsScore = s.channels * 10
        val bitsScore = s.bits_per_sample
        val sampleRateScore = s.sample_rate.toIntOrNull()?.div(1000) ?: 0

        return channelsScore + bitsScore + sampleRateScore
    }

    // ------------------------------------------------------------
    // VIDEO SELECTION
    // ------------------------------------------------------------

    private fun getVideoTrackToUse(streams: List<VideoStream>): Int {
        val selectStream = streams
            .filter { (it.duration_ts ?: 0) > 0 }
            .maxByOrNull { it.duration_ts ?: 0 }
            ?: streams.minByOrNull { it.index }
            ?: throw Exception("No video streams found")

        return streams.indexOf(selectStream)
    }
}
