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