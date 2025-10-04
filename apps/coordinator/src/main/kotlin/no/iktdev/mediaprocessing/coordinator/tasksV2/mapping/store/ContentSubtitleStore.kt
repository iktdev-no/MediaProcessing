package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.streamit.library.db.executeWithStatus
import no.iktdev.streamit.library.db.tables.content.SubtitleTable
import org.jetbrains.exposed.sql.insert
import java.io.File

object ContentSubtitleStore {
    val log = KotlinLogging.logger {}

    fun storeSubtitles(collection: String, destinationFile: File): Boolean {
        return executeWithStatus (getStoreDatabase().database, run =  {
            SubtitleTable.insert {
                it[this.associatedWithVideo] = destinationFile.nameWithoutExtension
                it[this.language] = destinationFile.parentFile.nameWithoutExtension
                it[this.collection] = collection
                it[this.format] = destinationFile.extension.uppercase()
                it[this.subtitle] = destinationFile.name
            }
        }, onError = {
            log.error { "Failed to store subtitle $destinationFile: ${it.message}" }
        })
    }

}