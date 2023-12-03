package no.iktdev.streamit.content.ui.dto

enum class SimpleEventDataState {
    NA,
    QUEUED,
    STARTED,
    ENDED,
    FAILED
}

fun toSimpleEventDataStateFromStatus(state: String?): SimpleEventDataState {
    return when(state) {
        "QUEUED" -> SimpleEventDataState.QUEUED
        "STARTED" -> SimpleEventDataState.STARTED
        "UPDATED" -> SimpleEventDataState.STARTED
        "FAILURE" -> SimpleEventDataState.FAILED
        "ENDED" -> SimpleEventDataState.ENDED
        else -> SimpleEventDataState.NA
    }
}

data class SimpleEventDataObject(
    val id: String,
    val name: String?,
    val path: String?,
    val givenTitle: String? = null,
    val givenSanitizedName: String? = null,
    val givenCollection: String? = null,
    val determinedType: String? = null,
    val eventEncoded: SimpleEventDataState = SimpleEventDataState.NA,
    val eventExtracted: SimpleEventDataState = SimpleEventDataState.NA,
    val eventConverted: SimpleEventDataState = SimpleEventDataState.NA,
    val eventCollected: SimpleEventDataState = SimpleEventDataState.NA,
    var encodingProgress: Int? = null,
    val encodingTimeLeft: Long? = null
)


data class EventDataObject(
    val id: String,
    var details: Details? = null,
    var metadata: Metadata? = null,
    var encode: Encode? = null,
    var io: IO? = null,
    var events: List<String> = emptyList()
) {
    fun toSimple() = SimpleEventDataObject(
        id = id,
        name = details?.name,
        path = details?.file,
        givenTitle = details?.title,
        givenSanitizedName = details?.sanitizedName,
        givenCollection = details?.collection,
        determinedType = details?.type,
        eventEncoded = toSimpleEventDataStateFromStatus(encode?.state),
        encodingProgress = encode?.progress,
        encodingTimeLeft = encode?.timeLeft
    )
}

data class Details(
    val name: String,
    val file: String,
    var title: String? = null,
    val sanitizedName: String,
    var collection: String? = null,
    var type: String? = null
)

data class Metadata(
    val source: String
)

interface ProcessableItem {
    var state: String
}

data class IO(
    val inputFile: String,
    val outputFile: String
)

data class Encode(
    override var state: String,
    var progress: Int = 0,
    var timeLeft: Long? = null
) : ProcessableItem