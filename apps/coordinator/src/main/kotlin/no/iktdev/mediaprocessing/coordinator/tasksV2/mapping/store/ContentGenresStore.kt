package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.streamit.library.db.query.GenreQuery

object ContentGenresStore {
    fun storeAndGetIds(genres: List<String>): String? {
        return try {
            withTransaction(getStoreDatabase()) {
                val gq = GenreQuery( *genres.toTypedArray() )
                gq.insertAndGetIds()
                gq.getIds().joinToString(",")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}