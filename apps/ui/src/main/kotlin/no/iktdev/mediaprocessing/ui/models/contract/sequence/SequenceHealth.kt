package no.iktdev.mediaprocessing.ui.models.contract.sequence

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