package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import java.io.File

class CollectProjection(val events: List<Event>) {

    val useFile: File? by lazy { projectUseFile() }
    val startedWith: StartProjection? by lazy { projectStartedWith() }
    var readStreamsTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set
    var metadataTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set
    var encodeTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set
    var extreactTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set
    var convertTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set
    var coverDownloadTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set
    var contentMigratedTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set
    var contentStoredTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set
    val metadata: MetadataProjection? by lazy { projectMetadata() }
    val processedMedia: ProcessedMediaProjection? by lazy { projectProcessedMedia() }
    val parsedFileInfo: ParsedFileInfoProjection? by lazy { projectParsedFileInfo() }

    init {
        val taskProjection = TaskProjection(events)
        readStreamsTaskStatus = taskProjection.projectStreamReadStatus()
        metadataTaskStatus = taskProjection.projectMetadataSearchStatus()
        encodeTaskStatus = taskProjection.projectEncodingPerformedStatus()
        extreactTaskStatus = taskProjection.projectExtractSubtitleStatus()
        convertTaskStatus = taskProjection.projectConvertStatus()
        coverDownloadTaskStatus = taskProjection.projectCoverDownloadStatus()
        contentMigratedTaskStatus = taskProjection.projectMigrateContentStatus()
        contentStoredTaskStatus = taskProjection.projectStoreContentAndMetadataStatus()
    }

    fun getTaskStatus(): List<TaskStatus> = listOf(
        metadataTaskStatus,
        encodeTaskStatus,
        extreactTaskStatus,
        convertTaskStatus,
        coverDownloadTaskStatus
    )

    fun getRelevantTaskStatuses(): List<TaskStatus> {
        val required = startedWith?.tasks ?: emptySet()

        val statusMap = mapOf(
            OperationType.Encode to encodeTaskStatus,
            OperationType.ExtractSubtitles to extreactTaskStatus,
            OperationType.ConvertSubtitles to convertTaskStatus,
            OperationType.MetadataSearch to metadataTaskStatus,
        )

        return required.map { statusMap[it] ?: TaskStatus.NotInitiated }
    }

    fun isWorkflowComplete(): Boolean {
        val statuses = getRelevantTaskStatuses()

        if (statuses.isEmpty()) return false

        val anyFailed = statuses.any { it == TaskStatus.Failed }
        val anyPending = statuses.any { it == TaskStatus.Pending }
        val allCompleted = statuses.all { it == TaskStatus.Completed }

        if (anyFailed) return false
        if (anyPending) return false

        return allCompleted
    }




    fun isStorePermitted(): Boolean {
        val start = events.filterIsInstance<StartProcessingEvent>().firstOrNull()
            ?: return false // ingen start → ingen store

        return when (start.data.flow) {
            StartFlow.Auto -> true
            StartFlow.Manual -> events.any { it is ManualAllowCompletionEvent }
            null -> false // eksplisitt: ukjent flow → ikke tillatt
        }
    }


    private fun projectUseFile(): File? {
        val added = events.filterIsInstance<FileAddedEvent>().firstOrNull()?.data
        val startEvent = projectStartedWith()
        return added?.fileUri?.let { File(it) } ?: if (startedWith != null) {
            startEvent?.inputFile
        } else null

    }

    private fun projectStartedWith(): StartProjection? {
        val startEvent = events.filterIsInstance<StartProcessingEvent>().firstOrNull() ?: return null
        return StartProjection(
            inputFile = startEvent.data.fileUri.let { File(it) },
            mode = startEvent.data.flow,
            tasks = startEvent.data.operation
        )
    }


    private fun projectMetadata(): MetadataProjection? {
        val metadataEvent = events.filterIsInstance<MetadataSearchResultEvent>().lastOrNull()
            ?: return null
        val coverDownloadResultEvents = events.filterIsInstance<CoverDownloadResultEvent>()
            .filter { it.status == no.iktdev.eventi.models.store.TaskStatus.Completed }
        val coverFile =
            coverDownloadResultEvents.find { it -> it.data?.source == metadataEvent.recommended?.metadata?.source }?.data?.outputFile
                ?.let { File(it) }
        val result = metadataEvent.recommended ?: return null
        return MetadataProjection(
            title = result.metadata.title,
            summary = result.metadata.summary,
            mediaType = result.metadata.type,
            genres = result.metadata.genres,
            cover = coverFile,
            source = result.metadata.source
        )
    }

    private fun projectProcessedMedia(): ProcessedMediaProjection? {
        val encodeEvent = events.filterIsInstance<ProcesserEncodeResultEvent>()
            .lastOrNull { it.status == no.iktdev.eventi.models.store.TaskStatus.Completed }
            ?: return null

        val extreactEvents = events.filterIsInstance<ProcesserExtractResultEvent>()
        val extractedFiles =
            if (extreactEvents.all { it.status == no.iktdev.eventi.models.store.TaskStatus.Completed }) {
                extreactEvents.mapNotNull { it.data?.cachedOutputFile?.let { filePath -> File(filePath) } }
            } else {
                emptyList()
            }

        val convertedEvents = events.filterIsInstance<ConvertTaskResultEvent>()
        val convertedFiles =
            if (convertedEvents.all { it.status == no.iktdev.eventi.models.store.TaskStatus.Completed }) {
                convertedEvents.flatMap { it.data?.outputFiles?.map { filePath -> File(filePath) } ?: emptyList() }
            } else {
                emptyList()
            }

        val encodedFile = encodeEvent.data?.cachedOutputFile?.let { File(it) }

        return ProcessedMediaProjection(
            encodedFile = encodedFile,
            extractedFiles = extractedFiles,
            convertedFiles = convertedFiles
        )
    }

    private fun projectParsedFileInfo(): ParsedFileInfoProjection? {
        val result = events.filterIsInstance<MediaParsedInfoEvent>().lastOrNull() ?: return null
        return ParsedFileInfoProjection(
            name = result.data.parsedFileName,
            collection = result.data.parsedCollection,
            mediaType = result.data.mediaType
        )
    }


    data class StartProjection(
        val inputFile: File,
        val mode: StartFlow,
        val tasks: Set<OperationType>
    )


    data class MetadataProjection(
        val title: String,
        val summary: List<MetadataSearchResultEvent.SearchResult.MetadataResult.Summary>,
        val mediaType: MediaType,
        val genres: List<String>,
        val cover: File?,
        val source: String
    )

    data class ParsedFileInfoProjection(
        val name: String,
        val collection: String,
        val mediaType: MediaType
    )

    data class ProcessedMediaProjection(
        val encodedFile: File?,
        val extractedFiles: List<File>,
        val convertedFiles: List<File>
    )


    enum class TaskStatus {
        NotInitiated,
        Pending,
        Completed,
        Failed
    }

    fun prettyPrint(): String = buildString {
        val startedContext = startedWith
        if (startedContext != null) {
            appendLine("📦 Project snapshot")
            appendLine("Started with: ${startedContext.inputFile.name} [mode=${startedContext.mode}, tasks=${startedContext.tasks}]")
            appendLine("Task statuses:")
            appendLine("  - Metadata: ${metadataTaskStatus.colored()}")
            appendLine("  - Encode:   ${encodeTaskStatus.colored()}")
            appendLine("  - Extract:  ${extreactTaskStatus.colored()}")
            appendLine("  - Convert:  ${convertTaskStatus.colored()}")
            appendLine("  - Cover:    ${coverDownloadTaskStatus.colored()}")

            metadata?.let {
                appendLine("Metadata:")
                appendLine("  • Title: ${it.title}")
                appendLine("  • Genres: ${it.genres.joinToString()}")
                appendLine("  • Source: ${it.source}")
                appendLine("  • Cover: ${it.cover?.path ?: "none"}")
            }

            parsedFileInfo?.let {
                appendLine("Parsed file info:")
                appendLine("  • Name: ${it.name}")
                appendLine("  • Collection: ${it.collection}")
                appendLine("  • Type: ${it.mediaType}")
            }

            processedMedia?.let {
                appendLine("Processed media:")
                appendLine("  • Encoded: ${it.encodedFile?.path ?: "none"}")
                appendLine("  • Extracted: ${it.extractedFiles.joinToString { f -> f.name }}")
                appendLine("  • Converted: ${it.convertedFiles.joinToString { f -> f.name }}")
            }
        } else {
            appendLine("Start event is missing, should not evaluate!")
        }

    }

    private fun TaskStatus.colored(): String = when (this) {
        TaskStatus.NotInitiated -> "\u001B[90m$this\u001B[0m" // grå
        TaskStatus.Pending -> "\u001B[33m$this\u001B[0m" // gul
        TaskStatus.Completed -> "\u001B[32m$this\u001B[0m" // grønn
        TaskStatus.Failed -> "\u001B[31m$this\u001B[0m" // rød
    }

}