package no.iktdev.streamit.content.reader.analyzer.encoding.dto

data class EncodeInformation(
    val inputFile: String,
    val outFileName: String,
    val language: String,
    val arguments: List<String>
)
