package no.iktdev.mediaprocessing.shared.common.dto

data class ProcessCoreInfo(
    val assigned: List<Int>?,
    val manual: List<Int>?,
    val effective: List<Int>?,
    val percent: Int?
)