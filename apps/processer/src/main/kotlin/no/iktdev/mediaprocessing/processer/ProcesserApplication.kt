package no.iktdev.mediaprocessing.processer

import mu.KotlinLogging
import no.iktdev.exfl.coroutines.CoroutinesDefault
import no.iktdev.exfl.coroutines.CoroutinesIO
import no.iktdev.exfl.observable.Observables
import no.iktdev.mediaprocessing.shared.common.DatabaseEnvConfig
import no.iktdev.eventi.database.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.database.cal.RunnerManager
import no.iktdev.mediaprocessing.shared.common.database.cal.TasksManager
import no.iktdev.mediaprocessing.shared.common.database.tables.runners
import no.iktdev.mediaprocessing.shared.common.database.tables.tasks
import no.iktdev.mediaprocessing.shared.common.getAppVersion
import no.iktdev.mediaprocessing.shared.common.toEventsDatabase
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled



private val logger = KotlinLogging.logger {}
val ioCoroutine = CoroutinesIO()
val defaultCoroutine = CoroutinesDefault()


@SpringBootApplication
class ProcesserApplication {
}

private lateinit var eventsDatabase: MySqlDataSource
fun getEventsDatabase(): MySqlDataSource {
    return eventsDatabase
}


lateinit var taskManager: TasksManager
lateinit var runnerManager: RunnerManager

private val log = KotlinLogging.logger {}


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

    runnerManager = RunnerManager(dataSource = getEventsDatabase(), name = ProcesserApplication::class.java.simpleName)
    runnerManager.assignRunner()

    runApplication<ProcesserApplication>(*args)
    log.info { "App Version: ${getAppVersion()}" }

}

@EnableScheduling
class DatabaseReconnect() {
    var lostConnectionCount = 0
    @Scheduled(fixedDelay = (100_000))
    fun checkIfConnected() {
        if (TransactionManager.currentOrNull() == null) {
            lostConnectionCount++
            eventsDatabase.toDatabase()
        }
    }
}