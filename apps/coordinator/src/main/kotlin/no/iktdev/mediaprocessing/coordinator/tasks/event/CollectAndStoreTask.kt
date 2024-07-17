package no.iktdev.mediaprocessing.coordinator.tasks.event

/*
@Service
class CollectAndStoreTask()  {
    val log = KotlinLogging.logger {}


    val producesEvent: KafkaEvents = KafkaEvents.EventCollectAndStore

    val requiredEvents: List<KafkaEvents> = listOf(
        EventMediaProcessStarted,
        EventMediaProcessCompleted
    )
    val listensForEvents: List<KafkaEvents> = KafkaEvents.entries



    fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {

        log.info { "${event.referenceId} triggered by ${event.event}" }

        val started = events.lastOrSuccessOf(EventMediaProcessStarted) ?: return null
        val completed = events.lastOrSuccessOf(EventMediaProcessCompleted) ?: return null
        if (!started.data.isSuccess() || !completed.data.isSuccess()) {
            return null
        }
        val mapped = ProcessMapping(events).map() ?: return null
        val collection = mapped.metadata?.collection ?: mapped.collection ?: return null

        val subtitlesStored = mapped.outputFiles?.subtitles?.let {
            storeSubtitles(collection = collection, subtitles = it)
        } ?: false

        val videoFile = mapped.outputFiles?.video?.let { File(it).name }
        val videoInfo = mapped.videoDetails


        val genres = mapped.metadata?.genres?.let {
            storeAndGetGenres(it)
        }

        val catalogId = mapped.metadata?.let { meta ->
            if (videoInfo == null || videoFile == null)
                null
            else
                storeCatalog(metadata = meta,genres = genres, videoFile = videoFile, videoDetails = videoInfo)
        } ?: return SimpleMessageData(Status.ERROR, "Unable to store catalog when metadata is null", event.eventId)

        mapped.metadata?.let {
            storeMetadata(catalogId = catalogId, metadata = it)
            storeTitles(it.collection, it.title, contentTitles = it.titles)
        }

        return SimpleMessageData(Status.COMPLETED, derivedFromEventId = event.eventId)
    }

    private fun storeSubtitles(collection: String, subtitles: List<String>): Boolean {
        val result = subtitles.map { subtitle ->
            val subtitleFile = File(subtitle)
            val language = subtitleFile.parentFile.name
            subtitle to executeWithStatus(getStoreDatabase()) {
                SubtitleQuery(
                    collection = collection,
                    associatedWithVideo = subtitleFile.nameWithoutExtension,
                    language = language,
                    format = subtitleFile.extension.uppercase(),
                    file = subtitleFile.name
                ).insert()
            }
        }
        return result.none { !it.second }
    }

    private fun storeMetadata(catalogId: Int, metadata: MetadataDto) {
        metadata.summary.forEach {
            withTransaction(getStoreDatabase()) {
                SummaryQuery(
                    cid = catalogId,
                    language = it.language,
                    description = it.summary
                ).insert()
            }
        }
    }

    private fun storeTitles(collection: String, usedTitle: String, contentTitles: List<String>) {
        try {
            withTransaction(getStoreDatabase()) {
                titles.insertIgnore {
                    it[titles.masterTitle] = collection
                    it[titles.title] = NameHelper.normalize(usedTitle)
                    it[titles.type] = 1
                }
                titles.insertIgnore {
                    it[titles.masterTitle] = usedTitle
                    it[titles.title] = NameHelper.normalize(usedTitle)
                    it[titles.type] = 2
                }
                contentTitles.forEach { title ->
                    titles.insertIgnore {
                        it[titles.masterTitle] = usedTitle
                        it[titles.title] = title
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun storeAndGetGenres(genres: List<String>): String? {
        return withTransaction(getStoreDatabase()) {
            val gq = GenreQuery( *genres.toTypedArray() )
            gq.insertAndGetIds()
            gq.getIds().joinToString(",")
        }
    }

    private fun storeCatalog(metadata: MetadataDto, videoDetails: VideoDetails, videoFile: String, genres: String?): Int? {
        val precreatedCatalogQuery = CatalogQuery(
                title = NameHelper.normalize(metadata.title),
                cover = metadata.cover?.cover,
                type = metadata.type,
                collection = metadata.collection,
                genres = genres
            )

        val result = when (videoDetails.type) {
            "serie" -> {
                val serieInfo = videoDetails.serieInfo ?: throw RuntimeException("SerieInfo missing in VideoDetails for Serie! $videoFile")
                executeOrException {
                    precreatedCatalogQuery.insertWithSerie(
                        episodeTitle = serieInfo.episodeTitle ?: "",
                        videoFile = videoFile,
                        episode = serieInfo.episodeNumber,
                        season = serieInfo.seasonNumber
                    )
                }
            }
            "movie" -> {
                executeOrException {
                    precreatedCatalogQuery.insertWithMovie(videoFile)
                }
            }
            else -> throw RuntimeException("${videoDetails.type} is not supported!")
        }
        val ignoreException = result?.cause is SQLIntegrityConstraintViolationException && (result as ExposedSQLException).errorCode == 1062
        return if (result == null || ignoreException ) {
            return withTransaction(getStoreDatabase()) {
                precreatedCatalogQuery.getId()
            }
        } else null
    }


}*/