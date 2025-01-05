package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import mu.KotlinLogging
import no.iktdev.eventi.database.executeOrException
import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.mediaprocessing.shared.common.contract.reader.MetadataDto
import no.iktdev.mediaprocessing.shared.common.contract.reader.VideoDetails
import no.iktdev.mediaprocessing.shared.common.parsing.NameHelper
import no.iktdev.streamit.library.db.insertWithSuccess
import no.iktdev.streamit.library.db.query.CatalogQuery
import no.iktdev.streamit.library.db.query.MovieQuery
import no.iktdev.streamit.library.db.query.SerieQuery
import no.iktdev.streamit.library.db.tables.catalog
import no.iktdev.streamit.library.db.tables.serie
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import java.sql.SQLIntegrityConstraintViolationException

object ContentCatalogStore {
    val log = KotlinLogging.logger {}

    /**
     * Given a list of titles and type,
     * the codes purpose is to find the matching collection in the catalog by title
     */
    fun getCollectionByTitleAndType(type: String, titles: List<String>): String? {
        return withTransaction(getStoreDatabase()) {
            catalog.select {
                (catalog.type eq type) and
                        ((catalog.title inList titles) or
                        (catalog.collection inList titles))
            }.map {
                it[catalog.collection]
            }.firstOrNull()
        }
    }

    private fun getCover(collection: String, type: String): String? {
        return withTransaction(getStoreDatabase()) {
            catalog.select {
                (catalog.collection eq collection) and
                        (catalog.type eq type)
            }.map { it[catalog.cover] }.firstOrNull()
        }
    }

    fun storeCatalog(title: String, collection: String, type: String, cover: String?, genres: String?): Int? {
        withTransaction(getStoreDatabase()) {
            val existingRow = catalog.select {
                (catalog.collection eq collection) and
                        (catalog.type eq type)
            }.firstOrNull()

            if (existingRow == null) {
                catalog.insertIgnore {
                    it[catalog.title] = title
                    it[catalog.cover] = cover
                    it[catalog.type] = type
                    it[catalog.collection] = collection
                    it[catalog.genres] = genres
                }
            } else {
                val id = existingRow[catalog.id]
                val storedTitle = existingRow[catalog.title]
                val useCover = existingRow[catalog.cover] ?: cover
                val useGenres = existingRow[catalog.genres] ?: genres

                catalog.update({
                    (catalog.id eq id) and
                    (catalog.collection eq collection)
                }) {
                    it[catalog.cover] = useCover
                    it[catalog.genres] = useGenres
                }
            }
        }
        return getId(title, collection, type)
    }

    private fun storeMovie(catalogId: Int, videoDetails: VideoDetails) {
        val iid = MovieQuery(videoDetails.fileName).insertAndGetId() ?: run {
            log.error { "Movie id was not returned!" }
            return
        }
        withTransaction(getStoreDatabase()) {
            catalog.update({
                (catalog.id eq catalogId)
            }) {
                it[catalog.iid] = iid
            }
        }
    }

    private fun storeSerie(collection: String, videoDetails: VideoDetails) {
        val serieInfo = videoDetails.serieInfo ?: run {
            log.error { "serieInfo in videoDetails is null!" }
            return
        }
        val status = insertWithSuccess(getStoreDatabase().database) {
            serie.insert {
                it[title] = serieInfo.episodeTitle
                it[episode] = serieInfo.episodeNumber
                it[season] = serieInfo.seasonNumber
                it[video] = videoDetails.fileName
                it[serie.collection] = collection
            }
        }
        if (!status) {
            log.error { "Failed to insert ${videoDetails.fileName} with episode: ${serieInfo.episodeNumber} and season ${serieInfo.seasonNumber}" }
            val finalStatus = insertWithSuccess(getStoreDatabase().database) {
                serie.insert {
                    it[title] = serieInfo.episodeTitle
                    it[episode] = serieInfo.episodeNumber
                    it[season] = 0
                    it[video] = videoDetails.fileName
                    it[serie.collection] = collection
                }
            }
            if (!finalStatus) {
                log.error { "Failed to insert ${videoDetails.fileName} with fallback season 0" }
            }
        }
    }

    fun storeMedia(title: String, collection: String, type: String, videoDetails: VideoDetails) {
        val catalogId = getId(title, collection, type) ?: return
        when (type) {
            "movie" -> storeMovie(catalogId, videoDetails)
            "serie" -> storeSerie(collection, videoDetails)
        }
    }

    fun getId(title: String, collection: String, type: String): Int? {
        return no.iktdev.streamit.library.db.withTransaction {
            catalog.select { catalog.title eq title }.andWhere {
                catalog.type eq type
            }.map { it[catalog.id].value }.firstOrNull()
        }
    }



}