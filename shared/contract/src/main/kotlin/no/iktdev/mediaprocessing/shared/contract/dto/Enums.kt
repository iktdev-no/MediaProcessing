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

enum class ProcessStartOperationEvents {
    ENCODE,
    EXTRACT,
    CONVERT
}

enum class RequestStartOperationEvents {
    CONVERT,
    EXTRACT,
}