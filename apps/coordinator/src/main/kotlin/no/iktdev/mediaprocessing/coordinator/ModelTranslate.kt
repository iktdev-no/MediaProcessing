package no.iktdev.mediaprocessing.coordinator

import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.model.SelectedAudioTracks
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.VideoCodecConfig
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.VideoCodecType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksEncodeSelectedEvent


fun no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecConfig.toDsl(): AudioCodec = when (type) {

    no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.AAC -> AudioCodec.Aac(
        bitrate = bitrate,
        profile = (profile ?: no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AacProfile.LC).translate(),
        channels = channels,
        sampleRate = sampleRate
    )

    no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.MP3 -> AudioCodec.Mp3(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate
    )

    no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.OPUS -> AudioCodec.Opus(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate,
        application = (application ?: no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.OpusApplication.Audio).translate()
    )

    no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.VORBIS -> AudioCodec.Vorbis(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate
    )

    no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.FLAC -> AudioCodec.Flac(
        compressionLevel = compressionLevel,
        channels = channels,
        sampleRate = sampleRate
    )

    no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.AC3 -> AudioCodec.Ac3(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate
    )

    no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.EAC3 -> AudioCodec.Eac3(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate
    )

    no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.DTS -> AudioCodec.Dts(
        bitrate = bitrate,
        channels = channels,
        sampleRate = sampleRate
    )

    no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.PCM -> AudioCodec.Pcm()

    no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.COPY -> AudioCodec.Copy(
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
        preset = (preset ?: no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.Presets.Slow).translate(),
        crf = crf ?: 18,
        bitrate = bitrate,
        tune = tune
    )

    VideoCodecType.H264 -> VideoCodec.H264(
        preset = (preset ?: no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.Presets.Slow).translate(),
        profile = (profile ?: no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.H264Profiles.High).translate(),
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
        preset = (preset ?: no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.Presets.Medium).translate(),
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