package no.iktdev.mediaprocessing.shared.kafka.dto


open class MessageDataWrapper(
    @Transient open val status: Status = Status.ERROR,
    @Transient open val message: String? = null
)



data class SimpleMessageData(
    override val status: Status,
    override val message: String? = null
) : MessageDataWrapper(status, message)


fun MessageDataWrapper?.isSuccess(): Boolean {
    return this != null && this.status != Status.ERROR
}