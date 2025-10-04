package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.streamit.library.db.tables.content.GenreTable
import org.jetbrains.exposed.sql.insertIgnoreAndGetId

object ContentGenresStore {
    fun storeAndGetIds(genres: List<String>): String? {
        return try {
            withTransaction(getStoreDatabase()) {
                val receivedGenreIdMap = genres.associateWith { genreName ->
                    GenreTable.insertIgnoreAndGetId { it[GenreTable.genre] = genreName }?.value
                }
                receivedGenreIdMap.values.filterNotNull()
                    .joinToString(",")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}