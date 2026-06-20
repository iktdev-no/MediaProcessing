package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.events.MultiTaskCreatorEventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.MultiTaskCreatedEvent
import no.iktdev.eventi.models.MultiTaskIdentity
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.requireAs
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream
import no.iktdev.mediaprocessing.ffmpeg.dsl.SubtitleCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.getSha256
import no.iktdev.mediaprocessing.shared.common.requireEventValue
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Component

@Component
class MediaCreateExtractTaskListener(): MultiTaskCreatorEventListener(EventStore, TaskStore) {
    private val log = KotlinLogging.logger {}

    fun toSubtitleArgumentData(index: Int, inputFile: IFile, outputFolderName: String, stream: SubtitleStream): ExtractSubtitleData? {
        val codec = SubtitleCodec.getCodec(stream.codec_name) ?: return null
        val extension = codec.getExtension()

        // ffmpeg-args for å mappe og copy akkurat dette subtitle-sporet
        val args = mutableListOf<String>()
        args += listOf("-map", "0:s:$index")
        args += codec.buildFfmpegArgs(stream)

        val language = stream.tags.language?: return null

        // outputfilnavn basert på index og extension
        val outputFileName = "${inputFile.nameWithoutExtension}-${language}.${extension}"

        val command = ffmpeg {
            input(inputFile.absolutePath) {
                subtitle(index) {
                    this.language = language
                }
            }
            output(outputFileName) {
                overwrite = true
                progress = false
            }
        }

        return ExtractSubtitleData(
            inputFile = inputFile.path,
            instructions = command.toInstructions(),
            outputFileName = outputFileName,
            outputFolderName = outputFolderName,
            language = language
        )

    }

    override fun isEventOfMyCreation(event: Event) =
        event is ProcesserExtractTaskCreatedEvent

    override fun onCreateTask(
        event: Event,
        history: List<Event>
    ): List<Task> {
        val selectedEvent = event.requireQualifiedEntry<MediaTracksExtractSelectedEvent>()


        val startedEvent = history.filterIsInstance<StartProcessingEvent>().firstOrNull() ?: return emptyList()
        if (startedEvent.data.operation.isNotEmpty()) {
            if (!startedEvent.data.operation.contains(OperationType.ExtractSubtitles))
                return emptyList()
        }

        val parsedInfo = history.getInstanceOf<MediaParsedInfoEvent>()?.data?.parsedFileName ?: run {
            log.error("Unable to get parsing info, this no output directory to use. Exiting listener")
            return emptyList()
        }


        val streams = history.requireEventValue<MediaStreamParsedEvent, ParsedMediaStreams> { it.data }

        val selectedStreams: Map<Int, SubtitleStream> =
            selectedEvent.selectedSubtitleTracks.mapNotNull { streamIndex ->
                val stream = streams.subtitleStream.firstOrNull { it.index == streamIndex }
                stream?.let { streams.subtitleStream.indexOf(it) to it }
            }.toMap()


        val preparedFileUri = history.requireEventValue<FilePrepareForWorkResultEvent, String> { it.file }
        val preparedFile = IFile(preparedFileUri)


        val entries = selectedStreams.mapNotNull { (idx, stream )->
            toSubtitleArgumentData(idx, preparedFile, parsedInfo ,stream,)
        }


        val tasks = entries.map { entry ->
            ExtractSubtitleTask(data = entry)
        }
        return tasks
    }

    override fun onTasksCreated(
        event: Event,
        history: List<Event>,
        tasks: List<Task>
    ): MultiTaskCreatedEvent {
        val createdEvent = ProcesserExtractTaskCreatedEvent(
            taskIds = tasks.map { MultiTaskIdentity(it.taskId, onGetTaskIdentity(it)) }.toSet()
        ).derivedOf(event) as MultiTaskCreatedEvent

        return createdEvent
    }

    override fun onGetTaskIdentity(task: Task): String {
        val t = task.requireAs<ExtractSubtitleTask>()
        val key = "${t.data.outputFolderName}::${t.data.language}::${t.data.outputFileName}"
        return key.getSha256()
    }


}