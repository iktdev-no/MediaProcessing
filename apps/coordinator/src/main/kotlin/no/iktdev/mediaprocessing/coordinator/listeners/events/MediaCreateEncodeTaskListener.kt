package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.coordinator.toDsl
import no.iktdev.mediaprocessing.coordinator.toFFmpegVersion
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.TranscodeDecision
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.plan.SimpleMediaPlan
import no.iktdev.mediaprocessing.ffmpeg.model.EncodeStrategy
import no.iktdev.mediaprocessing.ffmpeg.model.VideoTarget
import no.iktdev.mediaprocessing.ffmpeg.util.AudioTargeting
import no.iktdev.mediaprocessing.ffmpeg.util.CodecNameToFfmpegCodec
import no.iktdev.mediaprocessing.ffmpeg.util.determineEncodeStrategy
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.LinearEncodeTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.SegmentedEncodeTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.model.task.data.DefaultEncodeData
import no.iktdev.mediaprocessing.shared.common.requireEvent
import no.iktdev.mediaprocessing.shared.common.requireEventValue
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Component

@Component
class MediaCreateEncodeTaskListener(
    private val preference: Preference
) : EventListener() {

    private val log = KotlinLogging.logger {}

    override fun onEvent(event: Event,
        history: List<Event>
    ): Event? {
        val selectedEvent = event.requireQualifiedEntry<MediaTracksEncodeSelectedEvent>()

        val processerPreference = preference.getMediaPreference()
        val videoPreference = processerPreference.videoPreference?.codec?.toDsl() ?: VideoCodec.Hevc()

        val startedEvent = history.requireEvent<StartProcessingEvent>()
        if (startedEvent.data.operation.none { it == OperationType.Encode }) {
            return null
        }

        val streams = history.getInstanceOf<MediaStreamParsedEvent>()?.data ?: return null

        val audioTargets = AudioTargeting(streams.audioStream).getAudioTargets(
            selectedEvent.audioTracks.map { it.toFFmpegVersion() },
            processerPreference.audioPreference?.default?.toDsl() ?: AudioCodec.Aac(),
            processerPreference.audioPreference?.extended?.toDsl(),

            )
        val useVideoStream = streams.videoStream[selectedEvent.selectedVideoTrack]

        val transcodeDecision = videoPreference.determineTranscodeDecision(useVideoStream)
        val encodeStrategy = determineEncodeStrategy(transcodeDecision, useVideoStream)

        val useVideoCodec: VideoCodec = if (transcodeDecision == TranscodeDecision.Copy) VideoCodec.Copy else  videoPreference


        val videoTarget = VideoTarget(
            listIndex = selectedEvent.selectedVideoTrack,
            ffmpegIndex = streams.videoStream[selectedEvent.selectedVideoTrack].index,
            codec = useVideoCodec
        )
        val videoSourceCodec = CodecNameToFfmpegCodec(useVideoStream.codec_name)
        val planner = SimpleMediaPlan(sourceVideoCodec = videoSourceCodec, videoTarget, audioTargets, encodeStrategy)

        val extension = if (processerPreference.videoPreference?.enforceMkv == true) "mkv" else planner.toContainer()
        val preparedFile = history.requireEventValue<FilePrepareForWorkResultEvent, String> { it.file }

        val filename = IFile(preparedFile).nameWithoutExtension

        val parsedInfo = history.getInstanceOf<MediaParsedInfoEvent>()?.data?.parsedFileName ?: run {
            log.error("Unable to get parsing info, this no output directory to use. Exiting listener")
            return null
        }

        val outputFileName = "$filename.$extension"
        val outputFolderName = parsedInfo
        val inputFile = preparedFile

        val encodeData = when (planner) {
            is SimpleMediaPlan -> {
                val videoInstructs =
                    planner.toVideoInstructions(inputFile = inputFile, outputFile = outputFileName)
                val audioInstructs = planner.toAudioInstructions(inputFile = inputFile)
                DefaultEncodeData(
                    inputFile = inputFile,
                    outputFileName = outputFileName,
                    outputFolderName = outputFolderName,
                    videoInstruction = videoInstructs,
                    audioInstructions = audioInstructs,
                )
            }

            else -> throw RuntimeException("Unsupported planner type: ${planner::class.simpleName}")
        }

        val task = when (encodeStrategy) {
            EncodeStrategy.Linear -> LinearEncodeTask(encodeData)
            EncodeStrategy.Segmented -> SegmentedEncodeTask(encodeData)
        }

        val producerEvent = ProcesserEncodeTaskCreatedEvent(
            taskId = task.taskId,
            task::class.simpleName!!
        ).derivedOf(event)

        task.apply { derivedOf(producerEvent) }
        TaskStore.persist(task)

        return producerEvent
    }

}
