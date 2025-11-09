package no.iktdev.mediaprocessing.shared.common

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan("no.iktdev.mediaprocessing") // sikrer at common beans blir plukket opp
abstract class DatabaseApplication {
    companion object {
        inline fun <reified T : DatabaseApplication> launch(args: Array<String>) {
            runApplication<T>(*args)
        }
    }
}