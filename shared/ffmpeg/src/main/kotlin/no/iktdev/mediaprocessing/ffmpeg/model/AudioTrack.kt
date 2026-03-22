package no.iktdev.mediaprocessing.ffmpeg.model

data class AudioTrack(
        val arguments: List<String>,

        // Core metadata
        val language: String? = null,
        val title: String? = null,
        val isDefault: Boolean = false,
        val isForced: Boolean = false,

        // Recommended metadata
        val channels: Int? = null,          // 2, 6, 8
        val codec: String? = null,          // "aac", "ac3", "eac3"
        val bitrate: Int? = null,           // in kbps
        val isCommentary: Boolean = false,
        val isDescriptive: Boolean = false, // Audio Description

        // Advanced metadata
        val channelLayout: String? = null,  // "stereo", "5.1(side)"
        val sampleRate: Int? = null,        // 48000
        val bitDepth: Int? = null,          // 16, 24
        val isHearingImpaired: Boolean = false,
        val isOriginal: Boolean = false
    )