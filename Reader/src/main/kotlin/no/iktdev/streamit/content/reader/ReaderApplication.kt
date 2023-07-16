package no.iktdev.streamit.content.reader

import no.iktdev.streamit.content.reader.analyzer.PreferenceReader
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

@SpringBootApplication
class ReaderApplication

val preference = PreferenceReader().getPreference()
private var context: ApplicationContext? = null
fun getContext(): ApplicationContext? {
    return context
}
fun main(args: Array<String>) {

    context = runApplication<ReaderApplication>(*args)

}

