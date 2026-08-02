package no.iktdev.mediaprocessing.ui.models.contract.preferences.processer

data class CPULimit(
    var enabled: Boolean,
    var limit: Int = 100,
)