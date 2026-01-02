package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.model.MediaType


class MediaParsedInfoEvent(
    val data: ParsedData
): Event() {

    data class ParsedData(
        val parsedCollection: String,
        val parsedFileName: String,
        val parsedSearchTitles: List<String>,
        val mediaType: MediaType,
        val episodeInfo: EpisodeInfo? = null
    ) {
        data class EpisodeInfo(
            val episodeNumber: Int,
            val seasonNumber: Int,
            val episodeTitle: String? = null,
        )
    }
}

