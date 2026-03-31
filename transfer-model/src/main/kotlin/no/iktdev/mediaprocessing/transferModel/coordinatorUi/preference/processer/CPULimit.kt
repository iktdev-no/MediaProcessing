package no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.processer

data class CPULimit(
    var enabled: Boolean,
    var limit: Int = 100,
) {
    companion object {
        val default = CPULimit(false)
    }
}