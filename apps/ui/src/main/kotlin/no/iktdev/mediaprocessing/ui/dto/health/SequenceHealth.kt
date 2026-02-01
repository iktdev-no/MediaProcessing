package no.iktdev.mediaprocessing.ui.dto.health

import java.time.Duration
import java.time.Instant

data class SequenceHealth(
    val referenceId: String,

    val age: Duration,
    val expected: Duration,
    val lastEventAt: Instant,
    val eventCount: Int,

    // nye felter
    val startTime: Instant,
    val expectedFinishTime: Instant,
    val overdueDuration: Duration,
    val isOverdue: Boolean
)
