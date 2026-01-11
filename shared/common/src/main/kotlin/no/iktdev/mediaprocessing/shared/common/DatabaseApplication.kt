package no.iktdev.mediaprocessing.shared.common

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths
import no.iktdev.mediaprocessing.shared.common.configs.StreamItConfig
import org.jetbrains.exposed.sql.Database
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import javax.sql.DataSource

abstract class DatabaseApplication {
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
@SpringBootApplication
@ComponentScan("no.iktdev.mediaprocessing") // sikrer at common beans blir plukket opp
@Import(SharedConfig::class)
annotation class MediaProcessingApp

@Configuration
@EnableConfigurationProperties(
    value = [
        StreamItConfig::class,
        MediaPaths::class
    ]
)
class SharedConfig