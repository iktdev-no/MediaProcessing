package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksDetermineSubtitleTypeEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksExtractSelectedEvent
import no.iktdev.mediaprocessing.shared.common.model.SubtitleItem
import no.iktdev.mediaprocessing.shared.common.model.SubtitleType
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.SubtitleSelectionMode
import org.springframework.stereotype.Component

@Component
class MediaSelectExtractTracksListener(
    private val preference: Preference
) : EventListener() {

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event as? MediaTracksDetermineSubtitleTypeEvent ?: return null

        val pref = preference.getLanguagePreference()

        // 1. Filter by subtitle type (dialogue, forced, etc.)
        val filteredByType = filterBySelectionMode(
            items = useEvent.subtitleTrackItems,
            mode = pref.subtitleSelectionMode
        )

        // 2. Extract streams
        val streams = filteredByType.map { it.stream }

        // 3. Select subtitles based on language + format priority
        val selected = selectSubtitleStreams(
            streams = streams,
            preferredLanguages = pref.preferredSubtitles,
            preferOriginal = pref.preferOriginal,
            formatPriority = pref.subtitleFormatPriority
        )

        return MediaTracksExtractSelectedEvent(
            selectedSubtitleTracks = selected.map { it.index }
        ).derivedOf(event)
    }

    // ------------------------------------------------------------
    // TYPE FILTERING
    // ------------------------------------------------------------

    private fun filterBySelectionMode(
        items: List<SubtitleItem>,
        mode: SubtitleSelectionMode
    ): List<SubtitleItem> {
        return when (mode) {
            SubtitleSelectionMode.DialogueOnly ->
                items.filter { it.type == SubtitleType.Dialogue }

            SubtitleSelectionMode.DialogueAndForced ->
                items.filter {
                    it.type == SubtitleType.Dialogue || it.stream.disposition?.forced == 1
                }

            SubtitleSelectionMode.All ->
                items
        }
    }

    // ------------------------------------------------------------
    // SUBTITLE SELECTION LOGIC
    // ------------------------------------------------------------

    private fun selectSubtitleStreams(
        streams: List<SubtitleStream>,
        preferredLanguages: List<String>,
        preferOriginal: Boolean,
        formatPriority: List<String>
    ): List<SubtitleStream> {
        if (streams.isEmpty()) return emptyList()

        // 1. Originalspråk
        if (preferOriginal) {
            val originals = streams.filter { it.disposition?.original == 1 }
            if (originals.isNotEmpty()) {
                return originals.uniquePerLanguageBestFormat(formatPriority)
            }
        }

        // 2. Preferred languages
        for (lang in preferredLanguages) {
            val expanded = expandLanguage(lang)

            val match = streams.filter { s ->
                expanded.any { exp ->
                    s.tags.language?.equals(exp, ignoreCase = true) == true
                }
            }

            if (match.isNotEmpty()) {
                return match.uniquePerLanguageBestFormat(formatPriority)
            }
        }


        // 3. Default subtitles
        val defaults = streams.filter { it.disposition?.default == 1 }
        if (defaults.isNotEmpty()) {
            return defaults.uniquePerLanguageBestFormat(formatPriority)
        }

        // 4. Fallback: all subtitles
        return streams.uniquePerLanguageBestFormat(formatPriority)
    }

    private fun expandLanguage(code: String): List<String> =
        when (code.lowercase()) {
            "nor" -> listOf("nob", "nno")
            else -> listOf(code)
        }


    // ------------------------------------------------------------
    // UNIQUE PER LANGUAGE + FORMAT PRIORITY
    // ------------------------------------------------------------

    private fun List<SubtitleStream>.uniquePerLanguageBestFormat(
        formatPriority: List<String>
    ): List<SubtitleStream> {
        return this
            .groupBy { it.tags.language ?: "unknown" }
            .mapNotNull { (_, langGroup) ->
                langGroup
                    .sortedBy { s ->
                        val idx = formatPriority.indexOf(s.codec_name.lowercase())
                        if (idx == -1) Int.MAX_VALUE else idx
                    }
                    .firstOrNull()
            }
    }
}
