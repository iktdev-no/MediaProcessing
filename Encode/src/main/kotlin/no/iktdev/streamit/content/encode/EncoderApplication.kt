package no.iktdev.streamit.content.encode

import no.iktdev.exfl.observable.ObservableMap
import no.iktdev.exfl.observable.observableMapOf
import no.iktdev.streamit.content.common.dto.WorkOrderItem
import no.iktdev.streamit.content.encode.progress.Progress
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

@SpringBootApplication
class EncoderApplication

private var context: ApplicationContext? = null
val progressMap = observableMapOf<String, Progress>()

@Suppress("unused")
fun getContext(): ApplicationContext? {
    return context
}
fun main(args: Array<String>) {
    context = runApplication<EncoderApplication>(*args)
}

val encoderItems = ObservableMap<String, WorkOrderItem>()
val extractItems = ObservableMap<String, WorkOrderItem>()

/*val progress = ObservableMap<String, EncodeInformation>().also {
    it.addListener(object: ObservableMap.Listener<String, EncodeInformation> {
        override fun onPut(key: String, value: EncodeInformation) {
            super.onPut(key, value)
            logger.info { "$key with progress: $value." }
        }
    })
}*/