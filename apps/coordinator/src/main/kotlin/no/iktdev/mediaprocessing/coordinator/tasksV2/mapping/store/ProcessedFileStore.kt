package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.eventi.data.isSuccessful
import no.iktdev.mediaprocessing.coordinator.eventDatabase
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.dto.EventSummary
import no.iktdev.mediaprocessing.shared.common.database.tables.processedFile
import no.iktdev.mediaprocessing.shared.common.getChecksum
import no.iktdev.streamit.library.db.withTransaction
import org.jetbrains.exposed.sql.insert

object ProcessedFileStore {
    val log = KotlinLogging.logger {}

    fun store(title: String, events: List<Event>, summary: EventSummary) {
        val inputFilePath = events.findFirstEventOf<MediaProcessStartEvent>()?.data?.file ?: return
        val checksum = getChecksum(inputFilePath)


        withTransaction(eventDatabase.database.database, block = {
            processedFile.insert {
                it[this.title] = title
                it[this.inputFile] = inputFilePath
                it[this.data] = Gson().toJson(summary)
                it[this.checksum] = checksum
            }
        }) {
            it.printStackTrace()
        }

    }
}