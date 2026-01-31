package no.iktdev.mediaprocessing.coordinator.dto

import java.time.Duration
import java.time.Instant

data class SequenceHealth(
    val referenceId: String,
    val age: Duration,
    val expected: Duration,
    val lastEventAt: Instant,
    val eventCount: Int
)
