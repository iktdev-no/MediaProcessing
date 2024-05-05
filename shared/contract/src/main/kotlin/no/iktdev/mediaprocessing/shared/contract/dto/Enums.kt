package no.iktdev.mediaprocessing.shared.contract.dto

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

enum class StartOperationEvents {
    ENCODE,
    EXTRACT,
    CONVERT
}

fun List<StartOperationEvents>.isOnly(expected: StartOperationEvents): Boolean {
    return this.size == 1 && this.firstOrNull { it == expected } != null
}
