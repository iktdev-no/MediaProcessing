package no.iktdev.mediaprocessing.shared.kafka.dto

import com.google.gson.Gson
import no.iktdev.streamit.library.kafka.dto.Status
import java.io.Serializable
import java.lang.reflect.Type
import java.util.*


open class MessageDataWrapper(
    @Transient open val status: Status = Status.ERROR,
    @Transient open val message: String? = null
)



fun MessageDataWrapper?.isSuccess(): Boolean {
    return this != null && this.status != Status.ERROR
}