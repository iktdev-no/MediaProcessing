package no.iktdev.mediaprocessing

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.TestBase.DummyTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import java.util.*

object MockData {

    fun mediaParsedEvent(
        collection: String,
        fileName: String,
        mediaType: MediaType
    ) = MediaParsedInfoEvent(
        data = MediaParsedInfoEvent.ParsedData(
            parsedCollection = collection,
            parsedFileName = fileName,
            parsedSearchTitles = listOf(collection, fileName),
            mediaType = mediaType
        )
    )

    fun metadataEvent(derivedFrom: Event, source: String = "potetland", coverUrl: String = "cover.jpg"): List<Event> {
        val dummyTask = DummyTask().derivedOf(derivedFrom)
        val create = MetadataSearchTaskCreatedEvent(dummyTask.taskId).derivedOf(derivedFrom)

        val result = MetadataSearchResultEvent(
            results = listOf(
                MetadataSearchResultEvent.SearchResult(
                    simpleScore = 10,
                    prefixScore = 10,
                    advancedScore = 10,
                    sourceWeight = 1f,
                    data = MetadataSearchResultEvent.SearchResult.MetadataResult(
                        source = source,
                        title = "MyCollection",
                        cover = coverUrl,
                        type = MediaType.Movie,
                        summary = listOf(
                            MetadataSearchResultEvent.SearchResult.MetadataResult.Summary(
                                language = "en",
                                description = "desc"
                            )
                        ),
                        genres = listOf("Drama")
                    )
                )
            ),
            recommended = null,
            status = TaskStatus.Completed
        ).producedFrom(dummyTask)
        return listOf(create, result)
    }

    fun encodeEvent(cachedFile: String, derivedFrom: Event, status: TaskStatus = TaskStatus.Completed,): List<Event> {
        val dummyTask = DummyTask().derivedOf(derivedFrom)
        val create = ProcesserEncodeTaskCreatedEvent(dummyTask.taskId)
            .derivedOf(derivedFrom)

        val result = ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = cachedFile
            ),
            status = status
        ).producedFrom(dummyTask)
        return listOf(create, result)
    }

    fun extractEvent(language: String, cachedFile: String, derivedFrom: Event): List<Event> {
        val dummyTask = DummyTask().derivedOf(derivedFrom)
        val create = ProcesserExtractTaskCreatedEvent(listOf(dummyTask.taskId) as MutableList<UUID>)
            .derivedOf(derivedFrom)

        val result = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                language = language,
                cachedOutputFile = cachedFile
            )
        ).producedFrom(dummyTask)
        return listOf(create, result)
    }

    fun convertEvent(
        language: String,
        baseName: String,
        outputFiles: List<String>,
        derivedFrom: Event
    ): List<Event> {
        val dummyTask = DummyTask().derivedOf(derivedFrom)
        val createdTaskEvent = ConvertTaskCreatedEvent(
            taskId = dummyTask.taskId
        ).derivedOf(derivedFrom)

        val resultTask = ConvertTaskResultEvent(
            data = ConvertTaskResultEvent.ConvertedData(
                language = language,
                baseName = baseName,
                outputFiles = outputFiles
            ),
            status = TaskStatus.Completed
        ).producedFrom(dummyTask)
        return listOf(createdTaskEvent, resultTask)
    }

    fun coverEvent(cacheFile: String, derivedFrom: Event, source: String = "test"): List<Event> {
        val dummyTask = DummyTask().derivedOf(derivedFrom)
        val start = CoverDownloadTaskCreatedEvent(listOf(dummyTask.taskId)).derivedOf(derivedFrom)

        val result = CoverDownloadResultEvent(
            data = CoverDownloadResultEvent.CoverDownloadedData(
                source = source,
                outputFile = cacheFile
            ),
            status = TaskStatus.Completed
        ).producedFrom(dummyTask)
        return listOf(start, result)
    }

}