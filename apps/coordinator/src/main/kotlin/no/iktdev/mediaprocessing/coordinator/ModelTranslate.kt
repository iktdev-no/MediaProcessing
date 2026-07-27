package no.iktdev.mediaprocessing.coordinator

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.eventi.serialization.ZDS.toTask
import no.iktdev.mediaprocessing.coordinator.dto.LogAssociatedIds
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.model.SelectedAudioTracks
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksEncodeSelectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks_super.TransferTask
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.common.rules.TaskLifecycleRules
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CoordinatorTaskDto
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.MetadataDto
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceEvent
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.TaskStatus
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AacProfile
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecConfig
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.OpusApplication
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.H264Profiles
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.Presets
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.VideoCodecConfig
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.VideoCodecType
import kotlin.reflect.KProperty1

fun PersistedTask.toCoordinatorTransferDto(logs: List<LogAssociatedIds>): CoordinatorTaskDto {
    val matchingLogs = logs
        .filter { log -> log.ids.contains(taskId) }
        .map { it.logFile }

    val overrides = this.getOverrides()

    return CoordinatorTaskDto(
        id = id,
        referenceId = referenceId,
        status = status.name,
        taskId = taskId,
        task = task,
        data = data,
        claimed = claimed,
        claimedBy = claimedBy,
        consumed = consumed,
        lastCheckIn = lastCheckIn,
        persistedAt = persistedAt,
        logs = matchingLogs,
        abandoned = TaskLifecycleRules.isAbandoned(consumed, persistedAt, lastCheckIn),
        availableOverrides = overrides?.available ?: emptyList(),
        activeOverrides = overrides?.active ?: emptyList(),
    )
}

fun PersistedTask.getOverrides(): Overrides? {
    return when (val task = this.toTask()) {
        is TransferTask -> {
            val active = task.overrides?.map { it.name } ?: emptyList()
            val available = TransferTask.Overrides.entries
                .map { it.name }
                .filterNot { it in active }
            Overrides(available = available, active = active)
        }
        else -> null
    }
}

data class Overrides(val available: List<String>, val active: List<String>)


fun Event.extractPayload(): Map<String, Any?>? {
    val ignored = setOf("referenceId", "eventId", "metadata")

    return this::class.members
        .filterIsInstance<KProperty1<Event, *>>()
        .filter { it.name !in ignored }
        .associate { it.name to it.get(this) }
}


fun PersistedEvent.toDto(event: Event): SequenceEvent =
    SequenceEvent(
        eventId = this.eventId,
        referenceId = this.referenceId,
        type = this.event,
        timestamp = this.persistedAt,
        metadata = MetadataDto(
            derivedFromEventIds = event.metadata.derivedFromId,
            createdAt = event.metadata.created
        ),
        payload = event.extractPayload()
    )

fun CollectProjection.TaskStatus.translate(): TaskStatus {
    return TaskStatus.valueOf(this.name)
}

fun no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecConfig.toDsl(): AudioCodec = when (type) {

    _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.AAC -> AudioCodec.Aac(
        bitrate = bitrate,
        profile = (profile ?: _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AacProfile.LC).translate(),
        channels = channels,
        sampleRate = sampleRate
    )

    _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.MP3 -> AudioCodec.Mp3(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate
    )

    _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.OPUS -> AudioCodec.Opus(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate,
        application = (application ?: _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.OpusApplication.Audio).translate()
    )

    _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.VORBIS -> AudioCodec.Vorbis(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate
    )

    _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.FLAC -> AudioCodec.Flac(
        compressionLevel = compressionLevel,
        channels = channels,
        sampleRate = sampleRate
    )

    _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.AC3 -> AudioCodec.Ac3(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate
    )

    _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.EAC3 -> AudioCodec.Eac3(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate
    )

    _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.DTS -> AudioCodec.Dts(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate
    )

    _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.PCM -> AudioCodec.Pcm()

    _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.COPY -> AudioCodec.Copy(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate
    )
}


fun no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AacProfile.translate(): no.iktdev.mediaprocessing.ffmpeg.dsl.AacProfile {
    return no.iktdev.mediaprocessing.ffmpeg.dsl.AacProfile.valueOf(name)
}

fun no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.OpusApplication.translate(): no.iktdev.mediaprocessing.ffmpeg.dsl.OpusApplication {
    return no.iktdev.mediaprocessing.ffmpeg.dsl.OpusApplication.valueOf(name)
}

fun VideoCodecConfig.toDsl(): VideoCodec = when (type) {
    VideoCodecType.HEVC -> VideoCodec.Hevc(
        preset = (preset ?: _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.Presets.Slow).translate(),
        crf = crf ?: 18,
        bitrate = bitrate,
        tune = tune
    )

    VideoCodecType.H264 -> VideoCodec.H264(
        preset = (preset ?: _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.Presets.Slow).translate(),
        profile = (profile ?: _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.H264Profiles.High).translate(),
        level = level ?: 4.2,
        crf = crf ?: 23,
        bitrate = bitrate
    )

    VideoCodecType.VP9 -> VideoCodec.Vp9(
        crf = crf ?: 32,
        bitrate = bitrate,
        cpuUsed = cpuUsed ?: 4
    )

    VideoCodecType.VP8 -> VideoCodec.Vp8(
        crf = crf ?: 10,
        bitrate = bitrate
    )

    VideoCodecType.AV1 -> VideoCodec.Av1(
        crf = crf ?: 30,
        cpuUsed = cpuUsed ?: 4
    )

    VideoCodecType.VVC -> VideoCodec.Vvc(
        preset = (preset ?: _root_ide_package_.no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.Presets.Medium).translate(),
        crf = crf ?: 27,
        bitrate = bitrate
    )

    VideoCodecType.XVID -> VideoCodec.Vid(
        bitrate = bitrate,
        qscale = qscale
    )

    VideoCodecType.RAW -> VideoCodec.Raw
    VideoCodecType.COPY -> VideoCodec.Copy
}
fun no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.Presets.translate() = no.iktdev.mediaprocessing.ffmpeg.dsl.Presets.valueOf(name)
fun no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.H264Profiles.translate() = no.iktdev.mediaprocessing.ffmpeg.dsl.H264Profiles.valueOf(name)

fun MediaTracksEncodeSelectedEvent.SelectedAudioTracks.toFFmpegVersion(): SelectedAudioTracks {
    return SelectedAudioTracks(
        defaultListIndex = this.defaultListIndex,
        defaultFfmpegIndex = this.defaultFfmpegIndex,
        extendedListIndex = this.extendedListIndex,
        extendedFfmpegIndex = this.extendedFfmpegIndex,
    )
}