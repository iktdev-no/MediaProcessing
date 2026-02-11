package no.iktdev.mediaprocessing.transferModel.coordinatorUi

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = Success::class, name = "Success"),
    JsonSubTypes.Type(value = Failure::class, name = "Failure")
)
sealed interface DeleteResult {
    val type: String
}

data class Success(
    override val type: String = "Success"
) : DeleteResult

data class Failure(
    override val type: String = "Failure",
    val message: String
) : DeleteResult
