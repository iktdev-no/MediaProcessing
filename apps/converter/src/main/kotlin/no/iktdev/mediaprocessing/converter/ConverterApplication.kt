package no.iktdev.mediaprocessing.converter

import no.iktdev.mediaprocessing.shared.common.DatabaseEnvConfig
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentEventManager
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents
import no.iktdev.mediaprocessing.shared.common.toEventsDatabase
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

@SpringBootApplication
class ConvertApplication

private var context: ApplicationContext? = null
@Suppress("unused")
fun getContext(): ApplicationContext? {
    return context
}


lateinit var eventManager: PersistentEventManager

private lateinit var eventsDatabase: MySqlDataSource
fun getEventsDatabase(): MySqlDataSource {
    return eventsDatabase
}

fun main(args: Array<String>) {
    eventsDatabase = DatabaseEnvConfig.toEventsDatabase()
    eventsDatabase.createDatabase()
    eventsDatabase.createTables(processerEvents)

    eventManager = PersistentEventManager(eventsDatabase)

    context = runApplication<ConvertApplication>(*args)
}
//private val logger = KotlinLogging.logger {}