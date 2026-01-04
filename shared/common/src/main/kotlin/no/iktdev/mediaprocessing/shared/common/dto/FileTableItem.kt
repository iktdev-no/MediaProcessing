package no.iktdev.mediaprocessing.shared.common.dto

import java.time.LocalDateTime

data class FileTableItem(
    val name: String,
    val uri: String,
    val checksum: String,
    val identifiedAt: LocalDateTime,
)