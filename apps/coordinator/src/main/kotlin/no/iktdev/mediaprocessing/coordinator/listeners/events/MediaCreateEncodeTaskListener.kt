package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.events.SoftDispatchException
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.coordinator.toDsl
import no.iktdev.mediaprocessing.ffmpeg.dsl.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import no.iktdev.mediaprocessing.shared.common.requireEvent
import no.iktdev.mediaprocessing.shared.common.requireEventValue
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.springframework.stereotype.Component
import java.io.File

@Component
class MediaCreateEncodeTaskListener(
    private val preference: Preference
) : EventListener() {

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val preference = preference.getProcesserPreference()
        val videoDsl = preference.videoPreference?.codec?.toDsl()

        val startedEvent = history.filterIsInstance<StartProcessingEvent>().firstOrNull() ?: return null
        if (startedEvent.data.operation.isNotEmpty()) {
            if (!startedEvent.data.operation.contains(OperationType.Encode))
                return null
        }

        val selectedEvent = event as? MediaTracksEncodeSelectedEvent ?: return null
        val streams = history.filterIsInstance<MediaStreamParsedEvent>().firstOrNull()?.data ?: return null

        val videoPreference = videoDsl ?: VideoCodec.Hevc()

        val audioTargets = mutableListOf<AudioTarget>()

        for (track in selectedEvent.audioTracks) {

            // Default audio
            audioTargets += AudioTarget(
                listIndex = track.defaultListIndex,
                ffmpegIndex = track.defaultFfmpegIndex,
                codec = preference.audioPreference?.default?.toDsl()
                    ?: AudioCodec.Aac(channels = 2)
            )

            // Extended audio
            val extList = track.extendedListIndex
            val extFfmpeg = track.extendedFfmpegIndex

            if (extList != null && extFfmpeg != null) {
                audioTargets += AudioTarget(
                    listIndex = extList,
                    ffmpegIndex = extFfmpeg,
                    codec = preference.audioPreference?.extended?.toDsl()
                        ?: preference.audioPreference?.default?.toDsl()
                        ?: AudioCodec.Aac(channels = 2)
                )
            }

        }

        val plan = MediaPlan(
            videoTrack = VideoTarget(
                listIndex = selectedEvent.selectedVideoTrack,
                ffmpegIndex = streams.videoStream[selectedEvent.selectedVideoTrack].index,
                codec = videoPreference
            ),
            audioTracks = audioTargets
        )

        val args = plan.toFfmpegArgs(streams.videoStream, streams.audioStream)
        val extension = plan.toContainer()

        val preparedFile = history.requireEventValue<FilePrepareForWorkResultEvent, String> { it.file }

        val filename = File(preparedFile).nameWithoutExtension

        val task = EncodeTask(
            data = EncodeData(
                arguments = args,
                outputFileName = "$filename.$extension",
                inputFile = preparedFile
            )
        ).derivedOf(event)

        TaskStore.persist(task)

        return ProcesserEncodeTaskCreatedEvent(
            taskId = task.taskId
        ).derivedOf(event)
    }
}
