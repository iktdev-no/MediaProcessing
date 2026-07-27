package no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio

enum class OpusApplication(val ffmpegName: String) {
    Audio("audio"),      // Vanlig musikk/lyd
    Voip("voip"),        // Optimalisert for tale
    LowDelay("lowdelay") // Lav latency, f.eks. live streaming
}