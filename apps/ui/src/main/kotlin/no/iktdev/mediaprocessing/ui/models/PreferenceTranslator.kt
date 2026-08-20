package no.iktdev.mediaprocessing.ui.models

import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.CacheCleanupPreference
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.CleanupPreference
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.CoordinatorPreference
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.InputCleanupPreference
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.LanguagePreference
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.MediaPreference
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.Retention
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecConfig
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioPreference
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.VideoCodecConfig
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.VideoPreference
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.AacProfile
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.AudioCodecType
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.FlowTypes
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.H264Profiles
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.OpusApplication
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.Presets
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.RetentionUnit
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.SubtitleSelectionMode
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.VideoCodecType

fun LanguagePreference.translate(): no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.LanguagePreference {
    return no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.LanguagePreference(
        preferredAudio = this.preferredAudio,
        preferredSubtitles = this.preferredSubtitles,
        preferOriginal = this.preferOriginal,
        avoidDub = this.avoidDub,
        subtitleFormatPriority = this.subtitleFormatPriority,
        subtitleSelectionMode = SubtitleSelectionMode.valueOf(this.subtitleSelectionMode.name),
    )
}

fun no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.LanguagePreference.translate(): LanguagePreference {
    return LanguagePreference(
        preferredAudio = this.preferredAudio,
        preferredSubtitles = this.preferredSubtitles,
        preferOriginal = this.preferOriginal,
        avoidDub = this.avoidDub,
        subtitleFormatPriority = this.subtitleFormatPriority,
        subtitleSelectionMode = no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.SubtitleSelectionMode.valueOf(
            this.subtitleSelectionMode.name
        ),
    )
}

fun VideoPreference.translate(): no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.VideoPreference {
    return no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.VideoPreference(
        codec = this.codec.translate(),
        enforceMkv = this.enforceMkv
    )
}

fun no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.VideoPreference.translate(): VideoPreference {
    return VideoPreference(
        codec = this.codec.translate(),
        enforceMkv = this.enforceMkv
    )
}

fun VideoCodecConfig.translate(): no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.VideoCodecConfig {
    return no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.VideoCodecConfig(
        type = VideoCodecType.valueOf(this.type.name),
        crf = this.crf,
        bitrate = this.bitrate,
        preset = this.preset?.let { Presets.valueOf(it.name) },
        profile = this.profile?.let { H264Profiles.valueOf(it.name) },
        level = this.level,
        tune = this.tune,
        cpuUsed = this.cpuUsed,
        qscale = this.qscale,
        compressionLevel = this.compressionLevel
    )
}

fun no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.VideoCodecConfig.translate(): VideoCodecConfig {
    return VideoCodecConfig(
        type = no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.VideoCodecType.valueOf(this.type.name),
        crf = this.crf,
        bitrate = this.bitrate,
        preset = this.preset?.let {
            no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.Presets.valueOf(
                it.name
            )
        },
        profile = this.profile?.let {
            no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.H264Profiles.valueOf(
                it.name
            )
        },
        level = this.level,
        tune = this.tune,
        cpuUsed = this.cpuUsed,
        qscale = this.qscale,
        compressionLevel = this.compressionLevel
    )
}

fun AudioPreference.translate(): no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.AudioPreference {
    return no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.AudioPreference(
        default = this.default.translate(),
        extended = this.extended?.translate()
    )
}

fun no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.AudioPreference.translate(): AudioPreference {
    return AudioPreference(
        default = this.default.translate(),
        extended = this.extended?.translate()
    )
}

fun AudioCodecConfig.translate(): no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.AudioCodecConfig {
    return no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.AudioCodecConfig(
        type = AudioCodecType.valueOf(this.type.name),
        bitrate = this.bitrate,
        sampleRate = this.sampleRate,
        channels = this.channels,
        profile = this.profile?.let { AacProfile.valueOf(it.name) },
        application = this.application?.let { OpusApplication.valueOf(it.name) },
        compressionLevel = this.compressionLevel
    )
}

fun no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.AudioCodecConfig.translate(): AudioCodecConfig {
    return AudioCodecConfig(
        type = no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType.valueOf(this.type.name),
        bitrate = this.bitrate,
        sampleRate = this.sampleRate,
        channels = this.channels,
        profile = this.profile?.let {
            no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AacProfile.valueOf(
                it.name
            )
        },
        application = this.application?.let {
            no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.OpusApplication.valueOf(
                it.name
            )
        },
        compressionLevel = this.compressionLevel
    )
}

fun MediaPreference.translate(): no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.MediaPreference {
    return no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.MediaPreference(
        videoPreference = this.videoPreference?.translate(),
        audioPreference = this.audioPreference?.translate()
    )
}

fun no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.MediaPreference.translate(): MediaPreference {
    return MediaPreference(
        videoPreference = this.videoPreference?.translate(),
        audioPreference = this.audioPreference?.translate()
    )
}


fun CleanupPreference.translate(): no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.CleanupPreference {
    return no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.CleanupPreference(
        cacheCleanupPreference = this.cacheCleanupPreference.translate(),
        inputCleanupPreference = this.inputCleanupPreference.translate()
    )
}

fun no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.CleanupPreference.translate(): CleanupPreference {
    return CleanupPreference(
        cacheCleanupPreference = this.cacheCleanupPreference.translate(),
        inputCleanupPreference = this.inputCleanupPreference.translate()
    )
}

fun CacheCleanupPreference.translate(): no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.CacheCleanupPreference {
    return no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.CacheCleanupPreference(
        enabled = this.enabled,
        retention = this.retention.translate(),
        flows = FlowTypes.valueOf(this.flows.name)
    )
}

fun no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.CacheCleanupPreference.translate(): CacheCleanupPreference {
    return CacheCleanupPreference(
        enabled = this.enabled,
        retention = this.retention.translate(),
        flows = no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.FlowTypes.valueOf(this.flows.name)
    )
}

fun InputCleanupPreference.translate(): no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.InputCleanupPreference {
    return no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.InputCleanupPreference(
        enabled = this.enabled,
        retention = this.retention.translate(),
        flows = FlowTypes.valueOf(this.flows.name)
    )
}

fun no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.InputCleanupPreference.translate(): InputCleanupPreference {
    return InputCleanupPreference(
        enabled = this.enabled,
        retention = this.retention.translate(),
        flows = no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.FlowTypes.valueOf(this.flows.name)
    )
}

fun Retention.translate(): no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.Retention {
    return no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.Retention(
        value = this.value,
        unit = RetentionUnit.valueOf(this.unit.name)
    )
}

fun no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.Retention.translate(): Retention {
    return Retention(
        value = this.value,
        unit = no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.RetentionUnit.valueOf(this.unit.name)
    )
}

fun CoordinatorPreference.translate(): no.iktdev.mediaprocessing.ui.models.contract.preferences.CoordinatorPreference {
    return no.iktdev.mediaprocessing.ui.models.contract.preferences.CoordinatorPreference(
        language = this.language.translate(),
        media = this.media.translate(),
        cleanup = this.cleanup.translate(),
    )
}

fun no.iktdev.mediaprocessing.ui.models.contract.preferences.CoordinatorPreference.translate(): CoordinatorPreference {
    return CoordinatorPreference(
        language = this.language.translate(),
        media = this.media.translate(),
        cleanup = this.cleanup.translate(),
    )
}