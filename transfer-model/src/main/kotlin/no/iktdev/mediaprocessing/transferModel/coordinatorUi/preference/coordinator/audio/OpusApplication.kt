package no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.audio

enum class OpusApplication(val ffmpegName: String) {
    Audio("audio"),      // Vanlig musikk/lyd
    Voip("voip"),        // Optimalisert for tale
    LowDelay("lowdelay") // Lav latency, f.eks. live streaming
}