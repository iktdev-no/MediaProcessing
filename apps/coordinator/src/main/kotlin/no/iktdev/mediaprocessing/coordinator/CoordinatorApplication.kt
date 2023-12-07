package no.iktdev.mediaprocessing.coordinator


import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.common.DatabaseConfig
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.persistance.events
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

val log = KotlinLogging.logger {}

@SpringBootApplication
class CoordinatorApplication {
}

private var context: ApplicationContext? = null

@Suppress("unused")
fun getContext(): ApplicationContext? {
    return context
}

fun main(args: Array<String>) {
    // val dataSource = MySqlDataSource.fromDatabaseEnv();
    /*Coroutines.default().launch {
        dataSource.createDatabase()
        dataSource.createTables(
            events
        )
    }*/
    context = runApplication<CoordinatorApplication>(*args)
    printSharedConfig()
}

fun printSharedConfig() {
    log.info { "Kafka topic: ${SharedConfig.kafkaTopic}" }
    log.info { "File Input: ${SharedConfig.incomingContent}" }
    log.info { "File Output: ${SharedConfig.outgoingContent}" }
    log.info { "Ffprobe: ${SharedConfig.ffprobe}" }
    log.info { "Ffmpeg: ${SharedConfig.ffmpeg}" }

    log.info { "Database: ${DatabaseConfig.database}@${DatabaseConfig.address}:${DatabaseConfig.port}" }
    log.info { "Username: ${DatabaseConfig.username}" }
    log.info { "Password: ${ if(DatabaseConfig.password.isNullOrBlank()) "Is not set" else "Is set"}" }
}
