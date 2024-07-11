package no.iktdev.mediaprocessing.coordinator

import no.iktdev.mediaprocessing.shared.common.DatabaseEnvConfig
import no.iktdev.mediaprocessing.shared.common.persistance.allEvents
import no.iktdev.mediaprocessing.shared.common.persistance.events
import no.iktdev.mediaprocessing.shared.common.persistance.runners
import no.iktdev.mediaprocessing.shared.common.persistance.tasks
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