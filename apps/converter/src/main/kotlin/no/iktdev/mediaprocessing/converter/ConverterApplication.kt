package no.iktdev.mediaprocessing.converter

import mu.KotlinLogging
import no.iktdev.exfl.coroutines.CoroutinesDefault
import no.iktdev.exfl.coroutines.CoroutinesIO
import no.iktdev.exfl.observable.Observables
import no.iktdev.mediaprocessing.shared.common.DatabaseEnvConfig
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.getAppVersion
import no.iktdev.mediaprocessing.shared.common.persistance.*
import no.iktdev.mediaprocessing.shared.common.toEventsDatabase
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

@SpringBootApplication
class ConvertApplication

val ioCoroutine = CoroutinesIO()
val defaultCoroutine = CoroutinesDefault()
private var context: ApplicationContext? = null
@Suppress("unused")
fun getContext(): ApplicationContext? {
    return context
}

lateinit var taskManager: TasksManager
lateinit var runnerManager: RunnerManager


private lateinit var eventsDatabase: MySqlDataSource
private val log = KotlinLogging.logger {}

fun getEventsDatabase(): MySqlDataSource {
    return eventsDatabase
}

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
    eventsDatabase.createTables(tasks, runners)
    taskManager = TasksManager(eventsDatabase)

    runnerManager = RunnerManager(dataSource = getEventsDatabase(), name = ConvertApplication::class.java.simpleName)
    runnerManager.assignRunner()

    context = runApplication<ConvertApplication>(*args)
    log.info { "App Version: ${getAppVersion()}" }

}
//private val logger = KotlinLogging.logger {}