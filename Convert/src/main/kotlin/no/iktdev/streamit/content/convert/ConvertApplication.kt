package no.iktdev.streamit.content.convert

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

@SpringBootApplication
class ConvertApplication

private var context: ApplicationContext? = null
@Suppress("unused")
fun getContext(): ApplicationContext? {
    return context
}
fun main(args: Array<String>) {
    context = runApplication<ConvertApplication>(*args)
}
private val logger = KotlinLogging.logger {}