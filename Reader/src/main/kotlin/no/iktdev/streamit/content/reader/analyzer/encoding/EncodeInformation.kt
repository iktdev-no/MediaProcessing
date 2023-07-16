package no.iktdev.streamit.content.reader.analyzer.encoding

data class EncodeInformation(
    val inputFile: String,
    val outFileName: String,
    val language: String,
    val arguments: List<String>
)
