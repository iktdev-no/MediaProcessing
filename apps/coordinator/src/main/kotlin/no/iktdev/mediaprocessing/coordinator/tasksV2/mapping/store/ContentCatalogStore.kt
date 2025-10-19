package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import mu.KotlinLogging
import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.mediaprocessing.shared.common.contract.reader.VideoDetails
import no.iktdev.streamit.library.db.executeWithStatus
import no.iktdev.streamit.library.db.insertWithSuccess
import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.library.db.tables.content.MovieTable
import no.iktdev.streamit.library.db.tables.content.SerieTable
import org.jetbrains.exposed.sql.*

object ContentCatalogStore {
    val log = KotlinLogging.logger {}

    /**
     * Given a list of titles and type,
     * the codes purpose is to find the matching collection in the catalog by title
     */
    fun getCollectionByTitleAndType(type: String, titles: List<String>): String? {
        return withTransaction(getStoreDatabase()) {
            CatalogTable.selectAll().where {
                (CatalogTable.type eq type) and
                        ((CatalogTable.title inList titles) or
                        (CatalogTable.collection inList titles))
            }.map {
                it[CatalogTable.collection]
            }.firstOrNull()
        }
    }

    private fun getCover(collection: String, type: String): String? {
        return withTransaction(getStoreDatabase()) {
            CatalogTable.selectAll().where {
                (CatalogTable.collection eq collection) and
                        (CatalogTable.type eq type)
            }.map { it[CatalogTable.cover] }.firstOrNull()
        }
    }

    fun storeCatalog(title: String, titles: List<String>, collection: String, type: String, cover: String?, genres: String?): Int? {
        val status = executeWithStatus(getStoreDatabase().database, run = {
            val existingRow = CatalogTable.selectAll().where {
                (CatalogTable.collection eq collection) and
                        (CatalogTable.type eq type)
            }.firstOrNull()

            if (existingRow == null) {
                log.info { "$collection does not exist, and will be created" }
                CatalogTable.insert {
                    it[CatalogTable.title] = title
                    it[CatalogTable.cover] = cover
                    it[CatalogTable.type] = type
                    it[CatalogTable.collection] = collection
                    it[CatalogTable.genres] = genres
                }
            } else {
                val id = existingRow[CatalogTable.id]
                val storedTitle = existingRow[CatalogTable.title]
                val useCover = existingRow[CatalogTable.cover] ?: cover
                val useGenres = existingRow[CatalogTable.genres] ?: genres

                CatalogTable.update({
                    (CatalogTable.id eq id) and
                            (CatalogTable.collection eq collection)
                }) {
                    it[CatalogTable.cover] = useCover
                    it[CatalogTable.genres] = useGenres
                }
            }
        }, onError = {
            log.error { "Failed to store catalog $collection: ${it.message}" }
        })
        if (status) {
            log.info { "$collection was successfully stored!" }
        } else {
            log.error { "Unable to store catalog $collection..." }
        }
        return getId(title, titles, collection, type)
    }

    private fun storeMovie(catalogId: Int, videoDetails: VideoDetails) {
        val iid = MovieTable.insertAndGetId(videoDetails.fileName)?.value ?: run {
            log.error { "Movie id was not returned!" }
            return
        }
        val status = executeWithStatus(getStoreDatabase().database, run  = {
            CatalogTable.update({
                (CatalogTable.id eq catalogId)
            }) {
                it[CatalogTable.iid] = iid
            }
        }, onError = {
            log.error { "Failed to store movie ${videoDetails.fileName}: ${it.message}" }
        })
        if (status) {
            log.info { "${videoDetails.fileName} was successfully stored in movies!" }
        } else {
            log.error { "Unable to store catalog ${videoDetails.fileName} in movies..." }
        }
    }

    private fun storeSerie(collection: String, videoDetails: VideoDetails) {
        log.info { "Attempting to store $collection!" }
        val serieInfo = videoDetails.serieInfo ?: run {
            log.error { "serieInfo in videoDetails is null!" }
            return
        }
        val status = insertWithSuccess(getStoreDatabase().database, run = {
                SerieTable.insert {
                    it[title] = serieInfo.episodeTitle
                    it[episode] = serieInfo.episodeNumber
                    it[season] = serieInfo.seasonNumber
                    it[video] = videoDetails.fileName
                    it[SerieTable.collection] = collection
                }
            }, onError = {
                log.error { "Failed to store serie ${videoDetails.fileName}: ${it.message}" }
        })
        if (!status) {
            log.error { "Failed to insert ${videoDetails.fileName} with episode: ${serieInfo.episodeNumber} and season ${serieInfo.seasonNumber}" }
            val finalStatus = insertWithSuccess(getStoreDatabase().database, run =  {
                SerieTable.insert {
                    it[title] = serieInfo.episodeTitle
                    it[episode] = serieInfo.episodeNumber
                    it[season] = 0
                    it[video] = videoDetails.fileName
                    it[SerieTable.collection] = collection
                }
            }, onError = { log.error { "Failed to store serie: ${it.message}" } })
            if (!finalStatus) {
                log.error { "Failed to insert ${videoDetails.fileName} with fallback season 0" }
            } else {
                log.info { "${videoDetails.fileName} was successfully stored in movies with fallback season 0!" }
            }
        } else {
            log.info { "${videoDetails.fileName} was successfully stored in series!" }
        }
    }

    fun storeMedia(title: String, titles: List<String>, collection: String, type: String, videoDetails: VideoDetails) {
        val catalogId = getId(title, titles, collection, type) ?: run {
            log.warn { "Could not find id for $title with type $type" }
            return
        }
        log.info { "$title is identified as $type" }
        when (type) {
            "movie" -> storeMovie(catalogId, videoDetails)
            "serie" -> storeSerie(collection, videoDetails)
            else -> {
                log.error { "$type was provided for the function storeMedia, thus failing" }
                throw RuntimeException("Illegal type provided")
            }
        }
    }

    private fun getId(title: String, titles: List<String>, collection: String, type: String): Int? {
        val ids = withTransaction(getStoreDatabase().database) {
            CatalogTable.selectAll().where {
                ((CatalogTable.title eq title)
                        or (CatalogTable.collection eq collection)
                        or (CatalogTable.title inList titles)) and
                        (CatalogTable.type eq type)
            }.map { it[CatalogTable.id].value }
        } ?: run {
            log.warn { "No values found on $title with type $type" }
            return null
        }
        if (ids.size > 1) {
            log.info { "Found ids: ${ids.joinToString(",")}" }
        }
        return ids.firstOrNull()
    }



}