package no.iktdev.mediaprocessing.shared.kafka.dto


abstract class MessageDataWrapper(
    @Transient open val status: Status = Status.ERROR,
    @Transient open val message: String? = null,
    @Transient open val derivedFromEventId: String? = null
)

@Suppress("UNCHECKED_CAST")
fun <T> MessageDataWrapper.az(): T? {
    return try {
        this as T
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


data class SimpleMessageData(
    override val status: Status,
    override val message: String? = null,
    override val derivedFromEventId: String?
) : MessageDataWrapper(status, message, derivedFromEventId)


fun MessageDataWrapper?.isSuccess(): Boolean {
    return this != null && this.status == Status.COMPLETED
}

fun MessageDataWrapper?.isFailed(): Boolean {
    return if (this == null) true else this.status != Status.COMPLETED
}

fun MessageDataWrapper?.isSkipped(): Boolean {
    return this != null && this.status != Status.SKIPPED
}