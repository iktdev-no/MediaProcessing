package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksDetermineSubtitleTypeEvent
import no.iktdev.mediaprocessing.shared.common.model.SubtitleItem
import no.iktdev.mediaprocessing.shared.common.model.SubtitleType
import org.springframework.stereotype.Component

@Component
class MediaDetermineSubtitleTrackTypeListener: EventListener() {
    fun ignoreSHD(): Boolean = true
    fun ignoreCC(): Boolean = true
    fun ignoreSongs(): Boolean = true
    fun ignoreCommentary(): Boolean = true

    val supportedCodecs = setOf(
        "ass", "subrip", "webvtt", "vtt", "smi"
    )

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val useEvent = event as? MediaStreamParsedEvent ?: return null

        val collected = useEvent.data.subtitleStream
            .mapToType()
            .excludeTypes()
            .onlySupportedCodecs()


        return MediaTracksDetermineSubtitleTypeEvent(
            subtitleTrackItems = collected
        )
    }


    fun getCommentaryFilters(): Set<String> = setOf("commentary", "comentary", "kommentar", "kommentaar")
    fun getSongFilters(): Set<String> = setOf("song", "sign", "lyrics")
    fun getClosedCaptionFilters(): Set<String> = setOf("closed caption", "cc", "close caption", "closed-caption", "cc.")
    fun getSHDFilters(): Set<String> = setOf("shd", "hh", "hard of hearing", "hard-of-hearing")

    private fun List<SubtitleItem>.excludeTypes(): List<SubtitleItem> {
        return this.filter {
            when (it.type) {
                SubtitleType.Song -> !ignoreSongs()
                SubtitleType.Commentary -> !ignoreCommentary()
                SubtitleType.ClosedCaption -> !ignoreCC()
                SubtitleType.SHD -> !ignoreSHD()
                SubtitleType.Dialogue -> true
            }
        }
    }

    private fun List<SubtitleStream>.mapToType(): List<SubtitleItem> {
        return this.map {
            val title = it.tags.title?.lowercase() ?: ""
            val type = when {
                getCommentaryFilters().any { keyword -> title.contains(keyword) } -> SubtitleType.Commentary
                getSongFilters().any { keyword -> title.contains(keyword) } -> SubtitleType.Song
                getClosedCaptionFilters().any { keyword -> title.contains(keyword) } -> SubtitleType.ClosedCaption
                getSHDFilters().any { keyword -> title.contains(keyword) } -> SubtitleType.SHD
                else -> SubtitleType.Dialogue
            }
            SubtitleItem(it, type)
        }
    }

    private fun List<SubtitleItem>.onlySupportedCodecs(): List<SubtitleItem> {
        return this.filter { it.stream.codec_name in supportedCodecs }
    }

}