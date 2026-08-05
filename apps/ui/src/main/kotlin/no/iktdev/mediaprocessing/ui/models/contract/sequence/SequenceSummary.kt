package no.iktdev.mediaprocessing.ui.models.contract.sequence

import no.iktdev.mediaprocessing.ui.models.contract.MediaType


data class SequenceSummary(
    val title: String? = null,
    val collection: String? = null,
    val mediaType: MediaType? = null,
    val episodeInfo: EpisodeInfoSummary? = null,
    val metadata: MetadataSummary? = null, // Ny blokk for rå metadata
    val failingReasons: FailingReason? = null,
    val failedTasks: Set<String> = emptySet(),
    val availableActions: Set<SequenceActions> = setOf(SequenceActions.Delete)
) {
    data class EpisodeInfoSummary(
        val seasonNumber: Int,
        val episodeNumber: Int,
        val episodeTitle: String?
    )

    data class MetadataSummary(
        val title: String,
        val alternativeTitles: List<String>,
        val genres: List<String>,
        val source: String,
        val hasCover: Boolean
    )
}

enum class SequenceActions {
    Delete,
    Hold,
    Release
}

enum class FailingReason {
    MissingStart,
    NoRelevantTasksSet,
    TasksArePending,
    RequiredTasksHaveFailed,
    RequiredTasksAreNotComplete,
    RequiredOperationTasksHaveFailed
}