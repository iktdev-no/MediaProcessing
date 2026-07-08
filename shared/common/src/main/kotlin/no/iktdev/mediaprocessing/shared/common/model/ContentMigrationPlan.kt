package no.iktdev.mediaprocessing.shared.common.model

import no.iktdev.files.FileHash
import no.iktdev.mediaprocessing.shared.common.dto.files.HashedFile

data class ContentMigrationPlan(
    val collection: String,
    val videoContent: SingleContent? = null,
    val subtitleContent: List<SingleSubtitle>? = null, // Both extracted and converted
    val coverContent: SingleContent? = null
) {
    data class SingleContent(
        val cachedUri: String,
        val cacheHash: FileHash? = null,
        val storeUri: String
    )
    data class SingleSubtitle(
        val language: String,
        val cachedUri: String,
        val cacheHash: FileHash? = null,
        val storeUri: String
    )
}