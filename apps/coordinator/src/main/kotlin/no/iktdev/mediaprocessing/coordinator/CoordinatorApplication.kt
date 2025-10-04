package no.iktdev.mediaprocessing.coordinator


import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.CoroutinesDefault
import no.iktdev.exfl.coroutines.CoroutinesIO
import no.iktdev.exfl.observable.Observables
import no.iktdev.mediaprocessing.shared.common.*
import no.iktdev.eventi.database.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.database.cal.EventsManager
import no.iktdev.mediaprocessing.shared.common.database.cal.RunnerManager
import no.iktdev.mediaprocessing.shared.common.database.cal.TasksManager
import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.library.db.tables.content.GenreTable
import no.iktdev.streamit.library.db.tables.content.MovieTable
import no.iktdev.streamit.library.db.tables.content.ProgressTable
import no.iktdev.streamit.library.db.tables.content.SerieTable
import no.iktdev.streamit.library.db.tables.content.SubtitleTable
import no.iktdev.streamit.library.db.tables.content.SummaryTable
import no.iktdev.streamit.library.db.tables.content.TitleTable
import no.iktdev.streamit.library.db.tables.other.CastErrorTable
import no.iktdev.streamit.library.db.tables.other.DataAudioTable
import no.iktdev.streamit.library.db.tables.other.DataVideoTable
import no.iktdev.streamit.library.db.tables.user.UserTable
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.transaction.annotation.Transactional

val log = KotlinLogging.logger {}
lateinit var eventDatabase: EventsDatabase
private lateinit var eventsManager: EventsManager
lateinit var runnerManager: RunnerManager


@SpringBootApplication
class CoordinatorApplication {

    @Bean
    fun eventManager(): EventsManager {
        return eventsManager
    }

    @PreDestroy
    fun onShutdown() {
        doTransactionalCleanup()
    }

    @Transactional
    fun doTransactionalCleanup() {
        runnerManager.unlist()
    }

}

private lateinit var storeDatabase: MySqlDataSource

val ioCoroutine = CoroutinesIO().
        also {
            it.addListener(object : Observables.ObservableValue.ValueListener<Throwable> {
                override fun onUpdated(value: Throwable) {
                    log.error { "IO Coroutine" + value.printStackTrace() }
                }
            })
        }
val defaultCoroutine = CoroutinesDefault().
    also {
        it.addListener(object : Observables.ObservableValue.ValueListener<Throwable> {
            override fun onUpdated(value: Throwable) {
                log.error { "Default Coroutine" + value.printStackTrace() }
            }
        })
    }


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
        CatalogTable,
        GenreTable,
        MovieTable,
        SerieTable,
        SubtitleTable,
        SummaryTable,
        UserTable,
        ProgressTable,
        DataAudioTable,
        DataVideoTable,
        CastErrorTable,
        TitleTable
    )
    storeDatabase.createTables(*tables)

    runnerManager = RunnerManager(dataSource = eventDatabase.database, applicationName = CoordinatorApplication::class.java.simpleName)
    runnerManager.assignRunner()

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