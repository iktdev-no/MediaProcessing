package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.streamit.library.db.executeWithStatus
import no.iktdev.streamit.library.db.query.SubtitleQuery
import no.iktdev.streamit.library.db.tables.subtitle
import org.jetbrains.exposed.sql.insert
import java.io.File

object ContentSubtitleStore {
    val log = KotlinLogging.logger {}

    fun storeSubtitles(collection: String, language: String, destinationFile: File): Boolean {
        return executeWithStatus (getStoreDatabase().database, block =  {
            subtitle.insert {
                it[this.associatedWithVideo] = destinationFile.nameWithoutExtension
                it[this.language] = language
                it[this.collection] = collection
                it[this.format] = destinationFile.extension.uppercase()
                it[this.subtitle] = destinationFile.name
            }
        }, onError = {
            log.error { "Failed to store subtitle $destinationFile: ${it.message}" }
        })
    }

}