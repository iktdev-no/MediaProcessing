package no.iktdev.mediaprocessing.coordinator

import no.iktdev.mediaprocessing.shared.common.DatabaseEnvConfig
import no.iktdev.mediaprocessing.shared.common.database.tables.*
import no.iktdev.mediaprocessing.shared.common.toEventsDatabase

class EventsDatabase() {
    val database = DatabaseEnvConfig.toEventsDatabase()
    val tables = listOf(
        events, // For kafka
        allEvents,
        tasks,
        runners,
        processed
    )

    init {
        database.createDatabase()
        database.createTables(*tables.toTypedArray())
    }


}