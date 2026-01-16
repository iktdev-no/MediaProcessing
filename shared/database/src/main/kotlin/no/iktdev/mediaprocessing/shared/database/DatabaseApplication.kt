package no.iktdev.mediaprocessing.shared.database

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.MediaProcessingApp
import no.iktdev.mediaprocessing.shared.common.MediaProcessingApplication
import org.jetbrains.exposed.sql.Database
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.stereotype.Component
import javax.sql.DataSource

abstract class DatabaseApplication: MediaProcessingApplication() {
    companion object {
        inline fun <reified T : DatabaseApplication> launch(args: Array<String>) {
            runApplication<T>(*args)
        }
    }
}

@Component("ExposedInit")
class ExposedInitializer(
    private val dataSource: DataSource
) : InitializingBean {
    private val log = KotlinLogging.logger {}

    override fun afterPropertiesSet() {
        log.info { "Starting database connection" }
        Database.connect(dataSource)
    }
}


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MediaProcessingApp
@ComponentScan(
    basePackages = ["no.iktdev.mediaprocessing.shared.database"]
)
annotation class DatabasebasedMediaProcessingApp
