package no.iktdev.mediaprocessing.coordinator

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.mediaprocessing.coordinator.dto.LogAssociatedIds
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.common.rules.TaskLifecycleRules
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CoordinatorTaskDto
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.MetadataDto
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceEvent
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.TaskStatus
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AacProfile
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioCodecConfig
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioCodecType
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.OpusApplication
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.H264Profiles
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.Presets
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecConfig
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecType
import kotlin.reflect.KProperty1

fun PersistedTask.toCoordinatorTransferDto(logs: List<LogAssociatedIds>): no.iktdev.mediaprocessing.transferModel.coordinatorUi.CoordinatorTaskDto {
    val matchingLogs = logs
        .filter { log -> log.ids.contains(taskId) }
        .map { it.logFile }

    return _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.CoordinatorTaskDto(
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
        abandoned = TaskLifecycleRules.isAbandoned(consumed, persistedAt, lastCheckIn)
    )
}

fun Event.extractPayload(): Map<String, Any?>? {
    val ignored = setOf("referenceId", "eventId", "metadata")

    return this::class.members
        .filterIsInstance<KProperty1<Event, *>>()
        .filter { it.name !in ignored }
        .associate { it.name to it.get(this) }
}


fun PersistedEvent.toDto(event: Event): no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceEvent =
    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.SequenceEvent(
        eventId = this.eventId,
        referenceId = this.referenceId,
        type = this.event,
        timestamp = this.persistedAt,
        metadata = _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.MetadataDto(
            derivedFromEventIds = event.metadata.derivedFromId,
            createdAt = event.metadata.created
        ),
        payload = event.extractPayload()
    )

fun CollectProjection.TaskStatus.translate(): no.iktdev.mediaprocessing.transferModel.coordinatorUi.TaskStatus {
    return _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.TaskStatus.valueOf(this.name)
}

fun no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioCodecConfig.toDsl(): AudioCodec = when (type) {
    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioCodecType.AAC -> AudioCodec.Aac(
        bitrate = bitrate,
        profile = (profile ?: _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AacProfile.LC).translate(),
        channels = channels,
        sampleRate = sampleRate
    )
    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioCodecType.OPUS -> AudioCodec.Opus(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate,
        application = (application ?: _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.OpusApplication.Audio).translate()
    )
    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioCodecType.FLAC -> AudioCodec.Flac(
        compressionLevel = compressionLevel,
        channels = channels,
        sampleRate = sampleRate
    )
    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioCodecType.COPY -> AudioCodec.Copy
    else -> TODO("Implement remaining codecs")
}

fun no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AacProfile.translate(): no.iktdev.mediaprocessing.ffmpeg.dsl.AacProfile {
    return no.iktdev.mediaprocessing.ffmpeg.dsl.AacProfile.valueOf(name)
}

fun no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.OpusApplication.translate(): no.iktdev.mediaprocessing.ffmpeg.dsl.OpusApplication {
    return no.iktdev.mediaprocessing.ffmpeg.dsl.OpusApplication.valueOf(name)
}

fun no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecConfig.toDsl(): VideoCodec = when (type) {
    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecType.HEVC -> VideoCodec.Hevc(
        preset = (preset ?: _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.Presets.Slow).translate(),
        crf = crf ?: 18,
        bitrate = bitrate,
        tune = tune
    )

    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecType.H264 -> VideoCodec.H264(
        preset = (preset ?: _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.Presets.Slow).translate(),
        profile = (profile ?: _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.H264Profiles.High).translate(),
        level = level ?: 4.2,
        crf = crf ?: 23,
        bitrate = bitrate
    )

    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecType.VP9 -> VideoCodec.Vp9(
        crf = crf ?: 32,
        bitrate = bitrate,
        cpuUsed = cpuUsed ?: 4
    )

    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecType.VP8 -> VideoCodec.Vp8(
        crf = crf ?: 10,
        bitrate = bitrate
    )

    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecType.AV1 -> VideoCodec.Av1(
        crf = crf ?: 30,
        cpuUsed = cpuUsed ?: 4
    )

    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecType.VVC -> VideoCodec.Vvc(
        preset = (preset ?: _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.Presets.Medium).translate(),
        crf = crf ?: 27,
        bitrate = bitrate
    )

    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecType.XVID -> VideoCodec.Vid(
        bitrate = bitrate,
        qscale = qscale
    )

    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecType.RAW -> VideoCodec.Raw
    _root_ide_package_.no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecType.COPY -> VideoCodec.Copy
}
fun no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.Presets.translate() = no.iktdev.mediaprocessing.ffmpeg.dsl.Presets.valueOf(name)
fun no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.H264Profiles.translate() = no.iktdev.mediaprocessing.ffmpeg.dsl.H264Profiles.valueOf(name)
