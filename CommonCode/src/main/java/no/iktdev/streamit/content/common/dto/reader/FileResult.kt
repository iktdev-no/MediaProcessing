package no.iktdev.streamit.content.common.dto.reader

data class FileResult(
    val file: String,
    val title: String = "",
    val sanitizedName: String = ""
)