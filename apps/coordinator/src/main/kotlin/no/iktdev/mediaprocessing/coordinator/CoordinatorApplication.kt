package no.iktdev.mediaprocessing.coordinator


import mu.KotlinLogging
import no.iktdev.exfl.coroutines.CoroutinesDefault
import no.iktdev.exfl.coroutines.CoroutinesIO
import no.iktdev.exfl.observable.Observables
import no.iktdev.mediaprocessing.shared.common.*
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.persistance.*
import no.iktdev.streamit.library.db.tables.*
import no.iktdev.streamit.library.db.tables.helper.cast_errors
import no.iktdev.streamit.library.db.tables.helper.data_audio
import no.iktdev.streamit.library.db.tables.helper.data_video
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

val log = KotlinLogging.logger {}
private lateinit var eventDatabase: EventsDatabase
private lateinit var eventsManager: EventsManager

@SpringBootApplication
class CoordinatorApplication {

    @Bean
    fun eventManager(): EventsManager {
        return eventsManager
    }

}

private lateinit var storeDatabase: MySqlDataSource

val ioCoroutine = CoroutinesIO()
val defaultCoroutine = CoroutinesDefault()


fun getStoreDatabase(): MySqlDataSource {
    return storeDatabase
}

lateinit var taskManager: TasksManager

fun main(args: Array<String>) {


    printSharedConfig()

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
    eventDatabase = EventsDatabase().also {
        eventsManager = EventsManager(it.database)
    }



    storeDatabase = DatabaseEnvConfig.toStoredDatabase()
    storeDatabase.createDatabase()


    taskManager = TasksManager(eventDatabase.database)


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
    runApplication<CoordinatorApplication>(*args)
    log.info { "App Version: ${getAppVersion()}" }
}

fun printSharedConfig() {
    log.info { "File Input: ${SharedConfig.incomingContent}" }
    log.info { "File Output: ${SharedConfig.outgoingContent}" }
    log.info { "Ffprobe: ${SharedConfig.ffprobe}" }
    log.info { "Ffmpeg: ${SharedConfig.ffmpeg}" }

    /*log.info { "Database: ${DatabaseConfig.database} @ ${DatabaseConfig.address}:${DatabaseConfig.port}" }
    log.info { "Username: ${DatabaseConfig.username}" }
    log.info { "Password: ${if (DatabaseConfig.password.isNullOrBlank()) "Is not set" else "Is set"}" }*/
}
