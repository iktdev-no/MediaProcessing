package no.iktdev.streamit.content.reader.dto

data class CompletedItem(
    val name: String,
    val fullName: String,
    val time: String,
    val operations: List<CompletedTypes>
)

enum class CompletedTypes {
    ENCODE,
    EXTRACT,
    CONVERT
}