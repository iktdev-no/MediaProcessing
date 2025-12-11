package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioTarget
import no.iktdev.mediaprocessing.ffmpeg.dsl.MediaPlan
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoTarget
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksEncodeSelectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import no.iktdev.mediaprocessing.shared.common.stores.TaskStore
import org.springframework.stereotype.Component
import java.io.File

@Component
class MediaCreateEncodeTaskListener : EventListener() {


    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val preference = Preference.getProcesserPreference()

        val startedEvent = history.filterIsInstance<StartProcessingEvent>().firstOrNull() ?: return null
        val selectedEvent = event as? MediaTracksEncodeSelectedEvent ?: return null
        val streams = history.filterIsInstance<MediaStreamParsedEvent>().firstOrNull()?.data ?: return null

        val videoPreference = preference.videoPreference?.codec ?: VideoCodec.Hevc()
        val audioPreference = preference.audioPreference?.codec ?: AudioCodec.Aac(channels = 2)

        val audioTargets = mutableListOf<AudioTarget>(
            AudioTarget(
                index = selectedEvent.selectedAudioTrack,
                codec = audioPreference
            )
        )
        selectedEvent.selectedAudioExtendedTrack?.let {
            audioTargets.add(AudioTarget(
                index = it,
                codec = audioPreference
            ))
        }


        val plan = MediaPlan(
            videoTrack = VideoTarget(index = selectedEvent.selectedVideoTrack, codec = videoPreference),
            audioTracks = audioTargets
        )
        val args = plan.toFfmpegArgs(streams.videoStream, streams.audioStream)

        val task = EncodeTask(
            data = EncodeData(
                arguments = args,
                outputFileName = startedEvent.data.fileUri.let { File(it).nameWithoutExtension },
                inputFile = startedEvent.data.fileUri
            )
        ).derivedOf(event)


        TaskStore.persist(task)
        return null // Create task instead of event
    }



}