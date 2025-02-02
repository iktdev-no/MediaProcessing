package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.eventi.data.isSuccessful
import no.iktdev.mediaprocessing.coordinator.eventDatabase
import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.database.tables.processed
import no.iktdev.streamit.library.db.executeOrException
import no.iktdev.streamit.library.db.withTransaction
import org.jetbrains.exposed.sql.insert

object ProcessedItemsStore {
    val log = KotlinLogging.logger {}

    fun store(title: String, events: List<Event>, processedFiles: List<String>) {
        val inputFile = events.findFirstEventOf<MediaProcessStartEvent>()?.data?.file ?: return

        val isEncoded = events.findEventsOf<EncodeWorkPerformedEvent>().any { it.isSuccessful() }
        val isExtracted = events.findEventsOf<EncodeWorkPerformedEvent>().any { it.isSuccessful() }

        withTransaction(eventDatabase.database.database, block = {
            processed.insert {
                it[this.title] = title
                it[this.fileName] = inputFile
                it[this.processedFiles] = Gson().toJson(processedFiles)
                it[this.encoded] = isEncoded
                it[this.extracted] = isExtracted
            }
        }) {
            it.printStackTrace()
        }

    }
}