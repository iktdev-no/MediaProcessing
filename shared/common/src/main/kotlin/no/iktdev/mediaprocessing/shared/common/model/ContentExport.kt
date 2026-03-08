package no.iktdev.mediaprocessing.shared.common.model

import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent

data class ContentExport(
    val collection: String,
    val episodeInfo: EpisodeInfo? = null,
    val media: MediaExport? = null,
    val metadata: MetadataExport?
) {

    data class MetadataExport(
        // Vi tar ikke med collection fra metadata, da det bestemmes i Migrate
        val title: String,
        val alternativeTitles: List<String> = emptyList(),
        val genres: List<String> = emptyList(),
        val cover: String? = null,
        val summary: List<MetadataSearchResultEvent.SearchResult.MetadataResult.Summary> = emptyList(),
        val mediaType: MediaType,
        val source: String? = null
    )

    data class MediaExport(
        val videoFile: String?,
        val subtitles: List<Subtitle>,
    ) {
        data class Subtitle(
            val subtitleFile: String,
            val language: String
        )
    }
    data class EpisodeInfo(
        val episodeNumber: Int,
        val seasonNumber: Int,
        val episodeTitle: String? = null,
    )
}