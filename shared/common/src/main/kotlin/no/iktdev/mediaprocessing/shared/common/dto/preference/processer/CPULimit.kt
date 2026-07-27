package no.iktdev.mediaprocessing.shared.common.dto.preference.processer

data class CPULimit(
    var enabled: Boolean,
    var limit: Int = 100,
) {
    companion object {
        val default = CPULimit(false)
    }
}