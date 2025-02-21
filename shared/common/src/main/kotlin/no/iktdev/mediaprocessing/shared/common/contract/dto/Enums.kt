package no.iktdev.mediaprocessing.shared.common.contract.dto

enum class WorkStatus {
    Pending,
    Started,
    Working,
    Completed,
    Failed
}


enum class SubtitleFormats {
    ASS,
    SRT,
    VTT,
    SMI
}

enum class OperationEvents {
    ENCODE,
    EXTRACT,
    CONVERT
}

fun List<OperationEvents>.isOnly(expected: OperationEvents): Boolean {
    return this.size == 1 && this.firstOrNull { it == expected } != null
}
