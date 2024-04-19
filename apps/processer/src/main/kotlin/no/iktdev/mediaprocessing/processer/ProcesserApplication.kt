package no.iktdev.mediaprocessing.processer

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.DatabaseEnvConfig
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentEventManager
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents
import no.iktdev.mediaprocessing.shared.common.socket.SocketImplementation
import no.iktdev.mediaprocessing.shared.common.toEventsDatabase
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

private val logger = KotlinLogging.logger {}

@SpringBootApplication
class ProcesserApplication {
}

private lateinit var eventsDatabase: MySqlDataSource
fun getEventsDatabase(): MySqlDataSource {
    return eventsDatabase
}

lateinit var eventManager: PersistentEventManager


fun main(args: Array<String>) {
    eventsDatabase = DatabaseEnvConfig.toEventsDatabase()
    eventsDatabase.createDatabase()
    eventsDatabase.createTables(processerEvents)


    eventManager = PersistentEventManager(eventsDatabase)


    val context = runApplication<ProcesserApplication>(*args)
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