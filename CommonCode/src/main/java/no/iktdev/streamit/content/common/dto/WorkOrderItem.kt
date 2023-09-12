package no.iktdev.streamit.content.common.dto

data class WorkOrderItem(
    val id: String,
    val inputFile: String,
    val outputFile: String,
    val collection: String,
    val state: State,
    val progress: Int = 0,
    val remainingTime: Long? = null
)

enum class State {
    QUEUED,
    STARTED,
    UPDATED,
    FAILURE,
    ENDED
}