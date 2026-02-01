package no.iktdev.mediaprocessing.shared.common

import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths
import no.iktdev.mediaprocessing.shared.common.configs.StreamItConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

abstract class MediaProcessingApplication {
    companion object {
        inline fun <reified T : MediaProcessingApplication> launch(args: Array<String>) {
            runApplication<T>(*args)
        }
    }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootApplication
@ComponentScan(
    basePackages = ["no.iktdev.mediaprocessing.shared.common"]
) // sikrer at common beans blir plukket opp
@Import(SharedConfig::class)
annotation class MediaProcessingApp

@Configuration
@EnableConfigurationProperties(
    value = [
        MediaPaths::class
    ]
)
class SharedConfig