package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.coordinator.toDsl
import no.iktdev.mediaprocessing.coordinator.toFFmpegVersion
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.plan.LinearMediaPlan
import no.iktdev.mediaprocessing.ffmpeg.dsl.plan.SegmentedMediaPlan
import no.iktdev.mediaprocessing.ffmpeg.model.VideoTarget
import no.iktdev.mediaprocessing.ffmpeg.util.AudioTargeting
import no.iktdev.mediaprocessing.ffmpeg.util.getBestEncodeStrategy
import no.iktdev.mediaprocessing.ffmpeg.util.getMediaPlanner
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.LinearEncodeTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.SegmentedEncodeTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.model.task.data.EncodeDataBase
import no.iktdev.mediaprocessing.shared.common.model.task.data.LinearEncodeData
import no.iktdev.mediaprocessing.shared.common.model.task.data.SegmentEncodeData
import no.iktdev.mediaprocessing.shared.common.requireEvent
import no.iktdev.mediaprocessing.shared.common.requireEventValue
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Component
import java.io.File

@Component
class MediaCreateEncodeTaskListener(
    private val preference: Preference
) : EventListener() {

    private val log = KotlinLogging.logger {}

    override fun onEvent(event: Event,
        history: List<Event>
    ): Event? {
        val selectedEvent = event.requireQualifiedEntry<MediaTracksEncodeSelectedEvent>()

        val processerPreference = preference.getProcesserPreference()
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

        val videoTarget = VideoTarget(
            listIndex = selectedEvent.selectedVideoTrack,
            ffmpegIndex = streams.videoStream[selectedEvent.selectedVideoTrack].index,
            codec = videoPreference
        )

        val planner = getMediaPlanner(getBestEncodeStrategy(videoPreference, useVideoStream), videoTarget, audioTargets)

        val extension = planner.toContainer()
        val preparedFile = history.requireEventValue<FilePrepareForWorkResultEvent, String> { it.file }

        val filename = File(preparedFile).nameWithoutExtension

        val parsedInfo = history.getInstanceOf<MediaParsedInfoEvent>()?.data?.parsedFileName ?: run {
            log.error("Unable to get parsing info, this no output directory to use. Exiting listener")
            return null
        }

        val baseData = EncodeDataBase(
            outputFileName = "$filename.$extension",
            outputFolderName = parsedInfo,
            inputFile = preparedFile
        )

        val task = when (planner) {
            is SegmentedMediaPlan -> {
                val videoInstructs =
                    planner.toVideoInstructions(inputFile = baseData.inputFile, outputFile = baseData.outputFileName)
                val audioInstructs = planner.toAudioInstructions(inputFile = baseData.inputFile)
                val data = baseData.toSegmented(videoInstructs, audioInstructs)
                SegmentedEncodeTask(data)
            }

            is LinearMediaPlan -> {
                val arguments =
                    planner.toInstructions(inputFile = baseData.inputFile, outputFile = baseData.outputFileName)
                val data = baseData.toLinear(arguments)
                LinearEncodeTask(data)
            }

            else -> throw RuntimeException("Unsupported planner type: ${planner::class.simpleName}")
        }

        val producerEvent = ProcesserEncodeTaskCreatedEvent(
            taskId = task.taskId,
            task::class.simpleName!!
        ).derivedOf(event)

        task.apply { derivedOf(producerEvent) }
        TaskStore.persist(task)

        return producerEvent
    }

    fun EncodeDataBase.toLinear(instruct: FFmpegInstructions): LinearEncodeData {
        return LinearEncodeData(
            instructions = instruct,
            outputFileName = this.outputFileName,
            outputFolderName = this.outputFolderName,
            inputFile = this.inputFile
        )
    }

    fun EncodeDataBase.toSegmented(
        videoInstruction: FFmpegInstructions,
        audioInstructs: List<FFmpegInstructions>
    ): SegmentEncodeData {
        return SegmentEncodeData(
            videoInstruction = videoInstruction,
            audioInstructions = audioInstructs,
            outputFileName = this.outputFileName,
            outputFolderName = this.outputFolderName,
            inputFile = this.inputFile
        )
    }

}
