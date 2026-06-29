package no.iktdev.mediaprocessing.shared.database

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.MediaProcessingApp
import no.iktdev.mediaprocessing.shared.common.MediaProcessingApplication
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.jetbrains.exposed.sql.Database
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.env.Environment
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
    private val dataSource: DataSource,
    private val environment: Environment,
) : InitializingBean {
    private val log = KotlinLogging.logger {}

    override fun afterPropertiesSet() {
        if (environment.activeProfiles.contains("dry")) {
            EventStore.isDryMode = true
            TaskStore.isDryMode = true
        }

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
