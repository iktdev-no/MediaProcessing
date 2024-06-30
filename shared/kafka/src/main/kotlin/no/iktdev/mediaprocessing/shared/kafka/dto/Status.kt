package no.iktdev.mediaprocessing.shared.kafka.dto

enum class Status {
    SKIPPED,
    COMPLETED,
    ERROR
}

fun Status.isCompleted(): Boolean {
    return this == Status.COMPLETED
}