package no.iktdev.mediaprocessing.shared.common.model.views

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.model.MediaType

data class MetadataView(
        val title: String,
        val alternativeTitles: List<String> = emptyList(),
        val summary: List<MetadataSearchResultEvent.SearchResult.MetadataResult.Summary>,
        val mediaType: MediaType,
        val genres: List<String>,
        val cover: IFile?,
        val source: String
    )