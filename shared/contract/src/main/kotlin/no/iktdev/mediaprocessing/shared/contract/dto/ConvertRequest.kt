package no.iktdev.mediaprocessing.shared.contract.dto

data class ConvertRequest(
    val file: String, // FullPath
    val formats: List<SubtitleFormats>
)