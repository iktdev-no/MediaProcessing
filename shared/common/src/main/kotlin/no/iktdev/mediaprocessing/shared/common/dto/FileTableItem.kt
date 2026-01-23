package no.iktdev.mediaprocessing.shared.common.dto

import java.time.Instant

data class FileTableItem(
    val name: String,
    val uri: String,
    val checksum: String,
    val identifiedAt: Instant,
)