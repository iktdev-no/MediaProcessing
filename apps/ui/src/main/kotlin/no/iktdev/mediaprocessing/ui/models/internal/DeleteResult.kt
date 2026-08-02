package no.iktdev.mediaprocessing.ui.models.internal

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = DeleteResultSuccess::class, name = "Success"),
    JsonSubTypes.Type(value = DeleteResultFailure::class, name = "Failure")
)
sealed interface DeleteResult {
    val type: String
}

data class DeleteResultSuccess(
    override val type: String = "Success"
) : DeleteResult

data class DeleteResultFailure(
    override val type: String = "Failure",
    val message: String
) : DeleteResult
