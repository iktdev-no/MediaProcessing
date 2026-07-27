package no.iktdev.mediaprocessing.shared.common.dto.progress

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = EncodeProgress::class, name = "encode"),
    JsonSubTypes.Type(value = SimpleProgress::class, name = "simple"),
    JsonSubTypes.Type(value = FileCopyProgress::class, name = "filecopy")
)
sealed class Progress(val referenceId: String, val taskId: String, val progress: Int) {
}

class SimpleProgress(referenceId: String, taskId: String, progress: Int): Progress(referenceId, taskId, progress) {}
