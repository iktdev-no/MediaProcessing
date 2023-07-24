package no.iktdev.streamit.content.convert

import java.io.File

data class SubtitleInfo(
    val inputFile: File,
    val collection: String,
    val language: String
)