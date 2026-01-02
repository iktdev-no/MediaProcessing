package no.iktdev.mediaprocessing.shared.common

import org.jetbrains.exposed.sql.Database
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.stereotype.Component
import javax.sql.DataSource

abstract class DatabaseApplication {
    companion object {
        inline fun <reified T : DatabaseApplication> launch(args: Array<String>) {
            runApplication<T>(*args)
        }
    }
}

@Component
class ExposedInitializer(
    private val dataSource: DataSource
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        Database.connect(dataSource)
    }
}


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootApplication
@ComponentScan("no.iktdev.mediaprocessing") // sikrer at common beans blir plukket opp
annotation class MediaProcessingApp
