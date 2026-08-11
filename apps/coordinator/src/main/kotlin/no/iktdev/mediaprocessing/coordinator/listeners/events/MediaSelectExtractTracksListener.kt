package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.SubtitleSelectionMode
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksDetermineSubtitleTypeEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksExtractExcluded
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksExtractSelectedEvent
import no.iktdev.mediaprocessing.shared.common.model.SubtitleItem
import no.iktdev.mediaprocessing.shared.common.model.SubtitleType
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import org.springframework.stereotype.Component

@Component
class MediaSelectExtractTracksListener(
    private val preference: Preference
) : EventListener() {

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event.requireQualifiedEntry<MediaTracksDetermineSubtitleTypeEvent>()

        val pref = preference.getLanguagePreference()

        // 1. Filter by subtitle type (dialogue, forced, etc.)
        val filteredByType = filterBySelectionMode(
            items = useEvent.subtitleTrackItems,
            mode = pref.subtitleSelectionMode
        )

        // 2. Extract streams
        val streams = filteredByType.map { it.stream }

        // 3. Select subtitles based on language + format priority
        val result = selectSubtitleStreamsWithReason(
            streams = streams,
            preferredLanguages = pref.preferredSubtitles,
            preferOriginal = pref.preferOriginal,
            formatPriority = pref.subtitleFormatPriority
        )

        return MediaTracksExtractSelectedEvent(
            selectedSubtitleTracks = result.selected.map { it.index },
            excludedTracks = result.excluded
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

    data class SubtitleSelectionResult(
        val selected: List<SubtitleStream>,
        val excluded: List<MediaTracksExtractExcluded>
    )

    private fun selectSubtitleStreamsWithReason(
        streams: List<SubtitleStream>,
        preferredLanguages: List<String>,
        preferOriginal: Boolean,
        formatPriority: List<String>
    ): SubtitleSelectionResult {
        val selected = mutableListOf<SubtitleStream>()
        val excluded = mutableListOf<MediaTracksExtractExcluded>()

        if (streams.isEmpty()) {
            return SubtitleSelectionResult(emptyList(), emptyList())
        }

        val processedIndices = mutableSetOf<Int>()

        // Sørg for at engelsk alltid er med i tillegg til brukerens egne valg
        val effectiveLanguages = (preferredLanguages).distinct()

        // 1. Sjekk foretrukne språk (inkludert engelsk)
        for (lang in effectiveLanguages) {
            val expanded = expandLanguage(lang)
            val match = streams.filter { s ->
                s.index !in processedIndices && expanded.any { exp ->
                    s.tags.language?.equals(exp, ignoreCase = true) == true
                }
            }

            val bestForLanguage = match.uniquePerLanguageBestFormat(formatPriority)

            val losers = match.filter { it !in bestForLanguage }
            for (loser in losers) {
                excluded.add(MediaTracksExtractExcluded(loser.index, "Lost format priority for language $lang"))
                processedIndices.add(loser.index)
            }

            for (stream in bestForLanguage) {
                if (selected.none { it.tags.language == stream.tags.language }) {
                    selected.add(stream)
                    processedIndices.add(stream.index)
                } else {
                    excluded.add(MediaTracksExtractExcluded(stream.index, "Language already fulfilled by preferred list"))
                    processedIndices.add(stream.index)
                }
            }
        }

        // 2. Sjekk originalspråk for de som er igjen
        if (preferOriginal) {
            val originals = streams.filter { it.index !in processedIndices && it.disposition?.original == 1 }
            val bestOriginals = originals.uniquePerLanguageBestFormat(formatPriority)

            val losers = originals.filter { it !in bestOriginals }
            for (loser in losers) {
                excluded.add(MediaTracksExtractExcluded(loser.index, "Lost format priority for original language"))
                processedIndices.add(loser.index)
            }

            for (orig in bestOriginals) {
                if (selected.none { it.tags.language == orig.tags.language }) {
                    selected.add(orig)
                    processedIndices.add(orig.index)
                } else {
                    excluded.add(MediaTracksExtractExcluded(orig.index, "Language already fulfilled, skipping original"))
                    processedIndices.add(orig.index)
                }
            }
        }

        // 3. Fallback hvis ingenting er valgt
        val remaining = streams.filter { it.index !in processedIndices }
        if (selected.isEmpty()) {
            val defaults = remaining.filter { it.disposition?.default == 1 }
            val chosenDefaults = defaults.uniquePerLanguageBestFormat(formatPriority)

            selected.addAll(chosenDefaults)
            for (rem in remaining) {
                if (rem !in chosenDefaults) {
                    excluded.add(MediaTracksExtractExcluded(rem.index, "Fallback selection: did not match preferred/original/default criteria"))
                }
            }
        } else {
            for (rem in remaining) {
                excluded.add(MediaTracksExtractExcluded(rem.index, "Not matching preferred languages or original language preferences"))
            }
        }

        return SubtitleSelectionResult(selected, excluded)
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
