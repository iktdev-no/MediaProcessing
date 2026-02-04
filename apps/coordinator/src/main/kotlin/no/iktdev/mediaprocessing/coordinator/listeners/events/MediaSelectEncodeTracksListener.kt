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
        val audioTracks = getAudioTracksForAllPreferredLanguages(useEvent.data.audioStream)

        return MediaTracksEncodeSelectedEvent(
            selectedVideoTrack = videoTrackIndex,
            audioTracks = audioTracks
        ).derivedOf(event)
    }

    // ------------------------------------------------------------
    // NEW: MULTI-LANGUAGE AUDIO SELECTION
    // ------------------------------------------------------------

    private fun getAudioTracksForAllPreferredLanguages(
        audioStreams: List<AudioStream>
    ): List<MediaTracksEncodeSelectedEvent.SelectedAudioTracks> {

        if (audioStreams.isEmpty()) {
            return emptyList()
        }

        val pref = preference.getLanguagePreference()
        val result = mutableListOf<MediaTracksEncodeSelectedEvent.SelectedAudioTracks>()

        // ------------------------------------------------------------
        // 1. Forsøk å velge spor for alle foretrukne språk
        // ------------------------------------------------------------
        for (lang in pref.preferredAudio) {

            val langStreams = audioStreams.filter {
                it.tags.language?.equals(lang, ignoreCase = true) == true
            }

            if (langStreams.isEmpty()) continue

            val default = selectBestAudioStream(
                streams = langStreams,
                preferredLanguages = listOf(lang),
                preferOriginal = pref.preferOriginal,
                avoidDub = pref.avoidDub,
                mode = AudioSelectMode.DEFAULT
            )

            // Hvis avoidDub = true og default == null → ignorer språket
            if (default == null) continue


            val defaultListIndex = audioStreams.indexOf(default)
            val defaultFfmpegIndex = default.index

            val extended = selectBestAudioStream(
                streams = langStreams.filter { it.channels > 2 },
                preferredLanguages = listOf(lang),
                preferOriginal = pref.preferOriginal,
                avoidDub = pref.avoidDub,
                mode = AudioSelectMode.EXTENDED
            )

            val extendedListIndex = extended?.let { audioStreams.indexOf(it) }
            val extendedFfmpegIndex = extended?.index

            result.add(
                MediaTracksEncodeSelectedEvent.SelectedAudioTracks(
                    language = lang,
                    defaultListIndex = defaultListIndex,
                    defaultFfmpegIndex = defaultFfmpegIndex,
                    extendedListIndex = extendedListIndex,
                    extendedFfmpegIndex = extendedFfmpegIndex
                )
            )
        }

        // ------------------------------------------------------------
        // 2. Hvis ingen preferredLanguages ga treff → fallback
        // ------------------------------------------------------------
        if (result.isEmpty()) {

            // 2a. Hvis preferOriginal = true → velg original-spor
            if (pref.preferOriginal) {
                val originals = audioStreams.filter { it.disposition.original == 1 }
                if (originals.isNotEmpty()) {
                    val original = originals.minByOrNull { it.channels }!!
                    val lang = original.tags.language ?: "und"

                    result.add(
                        MediaTracksEncodeSelectedEvent.SelectedAudioTracks(
                            language = lang,
                            defaultListIndex = audioStreams.indexOf(original),
                            defaultFfmpegIndex = original.index,
                            extendedListIndex = null,
                            extendedFfmpegIndex = null
                        )
                    )

                    return result
                }
            }

            // 2b. Ellers → velg første tilgjengelige språk
            val first = audioStreams.first()
            val lang = first.tags.language ?: "und"

            result.add(
                MediaTracksEncodeSelectedEvent.SelectedAudioTracks(
                    language = lang,
                    defaultListIndex = audioStreams.indexOf(first),
                    defaultFfmpegIndex = first.index,
                    extendedListIndex = null,
                    extendedFfmpegIndex = null
                )
            )
        }

        return result
    }



    // ------------------------------------------------------------
    // ORIGINAL SELECTION LOGIC (unchanged)
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

        if (preferOriginal) {
            val originals = streams.filter { it.disposition.original == 1 }
            if (originals.isNotEmpty()) {
                return when (mode) {
                    AudioSelectMode.DEFAULT -> originals.minByOrNull { it.channels }
                    AudioSelectMode.EXTENDED -> originals.maxByOrNull { it.channels }
                }
            }
        }

        val filtered = if (avoidDub) {
            streams.filter { it.disposition.dub != 1 }
        } else streams

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

        val default = filtered.firstOrNull { it.disposition.default == 1 }
        if (default != null) return default

        return filtered.firstOrNull()
    }

    // ------------------------------------------------------------
    // VIDEO SELECTION (unchanged)
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
