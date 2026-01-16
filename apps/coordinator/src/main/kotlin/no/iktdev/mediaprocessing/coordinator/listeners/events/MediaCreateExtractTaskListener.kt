package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream
import no.iktdev.mediaprocessing.ffmpeg.dsl.SubtitleCodec
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksExtractSelectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.springframework.stereotype.Component
import java.io.File
import java.util.UUID

@Component
class MediaCreateExtractTaskListener: EventListener() {
    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {

        val startedEvent = history.filterIsInstance<StartProcessingEvent>().firstOrNull() ?: return null
        if (startedEvent.data.operation.isNotEmpty()) {
            if (!startedEvent.data.operation.contains(OperationType.Extract))
                return null
        }

        val selectedEvent = event as? MediaTracksExtractSelectedEvent ?: return null
        val streams = history.filterIsInstance<MediaStreamParsedEvent>().firstOrNull()?.data ?: return null

        val selectedStreams: Map<Int, SubtitleStream> = selectedEvent.selectedSubtitleTracks.associateWith {
            streams.subtitleStream[it]
        }

        val entries = selectedStreams.mapNotNull { (idx, stream )->
            toSubtitleArgumentData(idx, startedEvent.data.fileUri.let { File(it) }, stream)
        }

        val createdTaskIds: MutableList<UUID> = mutableListOf()
        entries.forEach { entry ->
            ExtractSubtitleTask(data = entry).derivedOf(event).also {
                TaskStore.persist(it)
                createdTaskIds.add(it.taskId)
            }
        }

        return ProcesserExtractTaskCreatedEvent(
            tasksCreated = createdTaskIds
        ).derivedOf(event)
    }

    fun toSubtitleArgumentData(index: Int, inputFile: File, stream: SubtitleStream): ExtractSubtitleData? {
        val codec = SubtitleCodec.getCodec(stream.codec_name) ?: return null
        val extension = codec.getExtension()

        // ffmpeg-args for å mappe og copy akkurat dette subtitle-sporet
        val args = mutableListOf<String>()
        args += listOf("-map", "0:s:$index")
        args += codec.buildFfmpegArgs(stream)

        val language = stream.tags.language?: return null

        // outputfilnavn basert på index og extension
        val outputFileName = "${inputFile.nameWithoutExtension}-${language}.${extension}"

        return ExtractSubtitleData(
            inputFile = inputFile.absolutePath,
            arguments = args,
            outputFileName = outputFileName,
            language = language
        )

    }


}