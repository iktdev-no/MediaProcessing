package no.iktdev.mediaprocessing.shared.kafka.dto


abstract class MessageDataWrapper(
    @Transient open val status: Status = Status.ERROR,
    @Transient open val message: String? = null
)



data class SimpleMessageData(
    override val status: Status,
    override val message: String? = null
) : MessageDataWrapper(status, message)


fun MessageDataWrapper?.isSuccess(): Boolean {
    return this != null && this.status == Status.COMPLETED
}

fun MessageDataWrapper?.isFailed(): Boolean {
    return if (this == null) true else this.status != Status.COMPLETED
}

fun MessageDataWrapper?.isSkipped(): Boolean {
    return this != null && this.status != Status.SKIPPED
}