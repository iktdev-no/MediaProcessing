package no.iktdev.mediaprocessing.shared.common.database

import no.iktdev.mediaprocessing.shared.common.DatabaseEnvConfig
import no.iktdev.mediaprocessing.shared.common.database.tables.allEvents
import no.iktdev.mediaprocessing.shared.common.database.tables.events
import no.iktdev.mediaprocessing.shared.common.database.tables.runners
import no.iktdev.mediaprocessing.shared.common.database.tables.tasks
import no.iktdev.mediaprocessing.shared.common.toEventsDatabase

class EventsDatabase() {
    val database = DatabaseEnvConfig.toEventsDatabase()
    val tables = listOf(
        events, // For kafka
        allEvents,
        tasks,
        runners
    )

    init {
        database.createDatabase()
        database.createTables(*tables.toTypedArray())
    }


}