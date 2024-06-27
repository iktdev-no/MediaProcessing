package no.iktdev.mediaprocessing.coordinator


import mu.KotlinLogging
import no.iktdev.exfl.coroutines.CoroutinesDefault
import no.iktdev.exfl.coroutines.CoroutinesIO
import no.iktdev.exfl.observable.Observables
import no.iktdev.mediaprocessing.shared.common.DatabaseEnvConfig
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.persistance.*
import no.iktdev.mediaprocessing.shared.common.toEventsDatabase
import no.iktdev.mediaprocessing.shared.common.toStoredDatabase
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
private lateinit var storeDatabase: MySqlDataSource
val ioCoroutine = CoroutinesIO()
val defaultCoroutine = CoroutinesDefault()

@Suppress("unused")
fun getContext(): ApplicationContext? {
    return context
}

fun getStoreDatabase(): MySqlDataSource {
    return storeDatabase
}

private lateinit var eventsDatabase: MySqlDataSource
fun getEventsDatabase(): MySqlDataSource {
    return eventsDatabase
}

lateinit var eventManager: PersistentEventManager
lateinit var taskManager: TasksManager

fun main(args: Array<String>) {
    ioCoroutine.addListener(listener = object: Observables.ObservableValue.ValueListener<Throwable> {
        override fun onUpdated(value: Throwable) {
            value.printStackTrace()
        }
    })
    defaultCoroutine.addListener(listener = object: Observables.ObservableValue.ValueListener<Throwable> {
        override fun onUpdated(value: Throwable) {
            value.printStackTrace()
        }
    })

    eventsDatabase = DatabaseEnvConfig.toEventsDatabase()
    eventsDatabase.createDatabase()

    storeDatabase = DatabaseEnvConfig.toStoredDatabase()
    storeDatabase.createDatabase()


    eventManager = PersistentEventManager(eventsDatabase)
    taskManager = TasksManager(eventsDatabase)


    val kafkaTables = listOf(
        events, // For kafka
        allEvents,
        tasks
    )


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
        cast_errors,
        titles
    )
    storeDatabase.createTables(*tables)


    eventsDatabase.createTables(*kafkaTables.toTypedArray())
    context = runApplication<CoordinatorApplication>(*args)
    printSharedConfig()
}

fun printSharedConfig() {
    log.info { "Kafka topic: ${KafkaEnv.kafkaTopic}" }
    log.info { "File Input: ${SharedConfig.incomingContent}" }
    log.info { "File Output: ${SharedConfig.outgoingContent}" }
    log.info { "Ffprobe: ${SharedConfig.ffprobe}" }
    log.info { "Ffmpeg: ${SharedConfig.ffmpeg}" }

    /*log.info { "Database: ${DatabaseConfig.database} @ ${DatabaseConfig.address}:${DatabaseConfig.port}" }
    log.info { "Username: ${DatabaseConfig.username}" }
    log.info { "Password: ${if (DatabaseConfig.password.isNullOrBlank()) "Is not set" else "Is set"}" }*/
}
