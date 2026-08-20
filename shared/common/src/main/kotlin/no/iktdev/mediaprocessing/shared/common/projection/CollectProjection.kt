package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.common.model.views.MetadataView
import no.iktdev.mediaprocessing.shared.common.model.views.ParsedFileInfoView
import no.iktdev.mediaprocessing.shared.common.model.views.ProcessedMediaView
import no.iktdev.mediaprocessing.shared.common.model.views.StartView
import no.iktdev.mediaprocessing.shared.common.projection.tasks.TaskProjection

class CollectProjection(val events: List<Event>) {

    val useFile: IFile? by lazy { projectUseFile() }
    val startedWith: StartView? by lazy { projectStartedWith() }
    val metadata: MetadataView? by lazy { projectMetadata() }
    val processedMedia: ProcessedMediaView? by lazy { projectProcessedMedia() }
    val parsedFileInfo: ParsedFileInfoView? by lazy { projectParsedFileInfo() }


    private fun projectUseFile(): IFile? {
        val added = events.filterIsInstance<FileAddedEvent>().firstOrNull()?.data
        val startEvent = projectStartedWith()
        return added?.fileUri?.let { IFile(it) } ?: if (startedWith != null) {
            startEvent?.inputFile
        } else null

    }

    private fun projectStartedWith(): StartView? {
        val startEvent = events.filterIsInstance<StartProcessingEvent>().firstOrNull() ?: return null
        return StartView(
            inputFile = startEvent.data.fileUri.let { IFile(it) },
            mode = startEvent.data.flow,
            tasks = startEvent.data.operation
        )
    }


    private fun projectMetadata(): MetadataView? {
        val startOperationTypes = events.getInstanceOf<StartProcessingEvent>()?.data?.operation ?: run {
            return null
        }
        val metadataEvent = events.filterIsInstance<MetadataSearchResultEvent>().lastOrNull()
            ?: return null
        val coverDownloadResultEvents = events.filterIsInstance<CoverDownloadResultEvent>()
            .filter { it.status == TaskStatus.Completed }
        val coverFile =
            coverDownloadResultEvents.find { it.data?.source == metadataEvent.recommended?.metadata?.source }?.data?.outputFile
                ?.let { IFile(it) }
        val result = metadataEvent.recommended ?: return null
        val useMediaType = if (startOperationTypes.isOnlySubtitles()) MediaType.Subtitle else result.metadata.type
        return MetadataView(
            title = result.metadata.title,
            alternativeTitles = result.metadata.alternateTitles,
            summary = result.metadata.summary,
            mediaType = useMediaType,
            genres = result.metadata.genres,
            cover = coverFile,
            source = result.metadata.source
        )
    }

    private fun projectProcessedMedia(): ProcessedMediaView? {
        val encodeEvent = events.filterIsInstance<ProcesserEncodeResultEvent>()
            .lastOrNull { it.status == no.iktdev.eventi.models.store.TaskStatus.Completed }
            ?: return null

        val extreactEvents = events.filterIsInstance<ProcesserExtractResultEvent>()
        val extractedFiles =
            if (extreactEvents.all { it.status == no.iktdev.eventi.models.store.TaskStatus.Completed }) {
                extreactEvents.mapNotNull { it.data?.cachedOutputFile?.let { filePath -> IFile(filePath) } }
            } else {
                emptyList()
            }

        val convertedEvents = events.filterIsInstance<ConvertTaskResultEvent>()
        val convertedFiles =
            if (convertedEvents.all { it.status == no.iktdev.eventi.models.store.TaskStatus.Completed }) {
                convertedEvents.flatMap { it.data?.outputFiles?.map { filePath -> IFile(filePath) } ?: emptyList() }
            } else {
                emptyList()
            }

        val encodedFile = encodeEvent.data?.cachedOutputFile?.let { IFile(it) }

        return ProcessedMediaView(
            encodedFile = encodedFile,
            extractedFiles = extractedFiles,
            convertedFiles = convertedFiles
        )
    }

    private fun projectParsedFileInfo(): ParsedFileInfoView? {
        val result = events.filterIsInstance<MediaParsedInfoEvent>().lastOrNull() ?: return null
        return ParsedFileInfoView(
            name = result.data.parsedFileName,
            collection = result.data.parsedCollection,
            mediaType = result.data.mediaType
        )
    }















    private fun TaskStatus.colored(): String = when (this) {
        TaskStatus.NotInitiated -> "\u001B[90m$this\u001B[0m" // grå
        TaskStatus.Pending -> "\u001B[33m$this\u001B[0m" // gul
        TaskStatus.InProgress -> "\u001B[33m$this\u001B[0m" // gul
        TaskStatus.Completed -> "\u001B[32m$this\u001B[0m" // grønn
        TaskStatus.Failed -> "\u001B[31m$this\u001B[0m" // rød
        TaskStatus.Cancelled -> "\u001B[90m$this\u001B[0m" // grå
        TaskStatus.Skipped -> "\u001B[90m$this\u001B[0m" // grå
    }

}