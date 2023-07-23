import mu.KotlinLogging
import no.iktdev.exfl.observable.ObservableMap
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

@SpringBootApplication
class EncoderApplication

private var context: ApplicationContext? = null

@Suppress("unused")
fun getContext(): ApplicationContext? {
    return context
}
fun main(args: Array<String>) {
    context = runApplication<EncoderApplication>(*args)
}
private val logger = KotlinLogging.logger {}

/*val progress = ObservableMap<String, EncodeInformation>().also {
    it.addListener(object: ObservableMap.Listener<String, EncodeInformation> {
        override fun onPut(key: String, value: EncodeInformation) {
            super.onPut(key, value)
            logger.info { "$key with progress: $value." }
        }
    })
}*/