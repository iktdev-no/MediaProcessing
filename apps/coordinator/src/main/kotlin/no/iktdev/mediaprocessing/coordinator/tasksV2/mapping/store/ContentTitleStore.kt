package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.mediaprocessing.shared.common.parsing.NameHelper
import no.iktdev.streamit.library.db.tables.titles
import no.iktdev.streamit.library.db.withTransaction
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select

object ContentTitleStore {

    fun store(mainTitle: String, otherTitles: List<String>) {
        try {
            withTransaction(getStoreDatabase().database, block = {
                val titlesToUse = otherTitles + listOf(
                    NameHelper.normalize(mainTitle)
                ).filter { it != mainTitle }

                titlesToUse.forEach { t ->
                    titles.insertIgnore {
                        it[masterTitle] = mainTitle
                        it[alternativeTitle] = t
                    }
                }
            }, {

            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun findMasterTitles(titleList: List<String>): List<String> {
        return withTransaction(getStoreDatabase().database, block = {
            titles.select {
                (titles.alternativeTitle inList titleList) or
                        (titles.masterTitle inList titleList)
            }.map {
                it[titles.masterTitle]
            }.distinctBy { it }
        }, {

        })  ?: emptyList()
    }
}