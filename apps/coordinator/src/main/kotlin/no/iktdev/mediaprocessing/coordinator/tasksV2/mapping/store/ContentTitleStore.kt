package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.mediaprocessing.shared.common.parsing.NameHelper
import no.iktdev.streamit.library.db.tables.content.TitleTable
import no.iktdev.streamit.library.db.withTransaction
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select

object ContentTitleStore {

    fun store(mainTitle: String, otherTitles: List<String>) {
        try {
            withTransaction(getStoreDatabase().database, run = {
                val titlesToUse = otherTitles + listOf(
                    NameHelper.normalize(mainTitle)
                ).filter { it != mainTitle }

                titlesToUse.forEach { t ->
                    TitleTable.insertIgnore {
                        it[masterTitle] = mainTitle
                        it[alternativeTitle] = t
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun findMasterTitles(titleList: List<String>): List<String> {
        return withTransaction(getStoreDatabase().database, run = {
            TitleTable.select {
                (TitleTable.alternativeTitle inList titleList) or
                        (TitleTable.masterTitle inList titleList)
            }.map {
                it[TitleTable.masterTitle]
            }.distinctBy { it }
        })  ?: emptyList()
    }
}