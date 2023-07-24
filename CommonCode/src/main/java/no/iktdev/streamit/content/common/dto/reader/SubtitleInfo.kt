package no.iktdev.streamit.content.common.dto.reader

import java.io.File

data class SubtitleInfo(
    val inputFile: String,
    val collection: String,
    val language: String
)