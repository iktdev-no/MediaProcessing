package no.iktdev.mediaprocessing.coordinator


import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.exfl.observable.Observables
import no.iktdev.mediaprocessing.shared.common.DatabaseConfig
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.persistance.events
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.streamit.library.db.tables.*
import no.iktdev.streamit.library.db.tables.helper.cast_errors
import no.iktdev.streamit.library.db.tables.helper.data_audio
import no.iktdev.streamit.library.db.tables.helper.data_video
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
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
    Coroutines.addListener(listener = object: Observables.ObservableValue.ValueListener<Throwable> {
        override fun onUpdated(value: Throwable) {
            value.printStackTrace()
        }
    })
    val dataSource = MySqlDataSource.fromDatabaseEnv();
    dataSource.createDatabase()

    val kafkaTables = listOf(
        events, // For kafka
    )

    dataSource.createTables(*kafkaTables.toTypedArray())

    val tables = arrayOf(
        catalog,
        genre,
        movie,
        serie,
        subtitle,
        summary,
        users,
        progress,
        data_audio,
        data_video,
        cast_errors
    )
    transaction {
        SchemaUtils.createMissingTablesAndColumns(*tables)
    }

    context = runApplication<CoordinatorApplication>(*args)
    printSharedConfig()
}

fun printSharedConfig() {
    log.info { "Kafka topic: ${KafkaEnv.kafkaTopic}" }
    log.info { "File Input: ${SharedConfig.incomingContent}" }
    log.info { "File Output: ${SharedConfig.outgoingContent}" }
    log.info { "Ffprobe: ${SharedConfig.ffprobe}" }
    log.info { "Ffmpeg: ${SharedConfig.ffmpeg}" }

    log.info { "Database: ${DatabaseConfig.database}@${DatabaseConfig.address}:${DatabaseConfig.port}" }
    log.info { "Username: ${DatabaseConfig.username}" }
    log.info { "Password: ${if (DatabaseConfig.password.isNullOrBlank()) "Is not set" else "Is set"}" }
}
