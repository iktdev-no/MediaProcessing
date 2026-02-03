package no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio

enum class AacProfile(val ffmpegName: String) {
    LC("aac_low"),   // Low Complexity – mest brukt
    HE("aac_he"),    // High Efficiency – bedre komprimering
    HEv2("aac_he_v2") // High Efficiency v2 – enda mer komprimering
}