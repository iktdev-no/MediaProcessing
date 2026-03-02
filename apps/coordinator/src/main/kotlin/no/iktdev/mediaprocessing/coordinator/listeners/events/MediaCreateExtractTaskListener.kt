package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams
import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream
import no.iktdev.mediaprocessing.ffmpeg.dsl.SubtitleCodec
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.requireEventValue
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Component
import java.io.File
import java.util.*

@Component
class MediaCreateExtractTaskListener(): EventListener() {
    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val selectedEvent = event.requireQualifiedEntry<MediaTracksExtractSelectedEvent>()


        val startedEvent = history.filterIsInstance<StartProcessingEvent>().firstOrNull() ?: return null
        if (startedEvent.data.operation.isNotEmpty()) {
            if (!startedEvent.data.operation.contains(OperationType.ExtractSubtitles))
                return null
        }

        val parsedInfo = history.getInstanceOf<MediaParsedInfoEvent>()?.data?.parsedFileName


        val streams = history.requireEventValue<MediaStreamParsedEvent, ParsedMediaStreams> { it.data }

        val selectedStreams: Map<Int, SubtitleStream> =
            selectedEvent.selectedSubtitleTracks.mapNotNull { streamIndex ->
                val stream = streams.subtitleStream.firstOrNull { it.index == streamIndex }
                stream?.let { streams.subtitleStream.indexOf(it) to it }
            }.toMap()


        val preparedFileUri = history.requireEventValue<FilePrepareForWorkResultEvent, String> { it.file }
        val preparedFile = File(preparedFileUri)


        val entries = selectedStreams.mapNotNull { (idx, stream )->
            toSubtitleArgumentData(idx, preparedFile, parsedInfo ,stream,)
        }


        val tasks = entries.map { entry ->
            ExtractSubtitleTask(data = entry)
        }

        val createdEvent = ProcesserExtractTaskCreatedEvent(
            taskIds = tasks.map { it -> it.taskId }
        ).derivedOf(event)

        tasks.forEach { task ->
            task.apply { derivedOf(createdEvent) }
            TaskStore.persist(task)
        }

        return createdEvent
    }

    fun toSubtitleArgumentData(index: Int, inputFile: File, outputFolderName: String?, stream: SubtitleStream): ExtractSubtitleData? {
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
            inputFile = inputFile.path,
            arguments = args,
            outputFileName = outputFileName,
            outputFolderName = outputFolderName,
            language = language
        )

    }


}