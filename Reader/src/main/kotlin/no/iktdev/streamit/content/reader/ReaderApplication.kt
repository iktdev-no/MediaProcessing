package no.iktdev.streamit.content.reader

import no.iktdev.streamit.content.reader.analyzer.PreferenceReader
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class ReaderApplication

val preference = PreferenceReader().getPreference()

fun main(array: Array<String>) {



}

