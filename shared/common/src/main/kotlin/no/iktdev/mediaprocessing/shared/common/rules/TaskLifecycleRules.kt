package no.iktdev.mediaprocessing.shared.common.rules

import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.*
import no.iktdev.mediaprocessing.shared.common.getName
import java.time.Instant
import java.time.temporal.ChronoUnit

object TaskLifecycleRules {
    const val abandonedAfterMinutes = 15L

    fun isAbandoned(
        consumed: Boolean,
        createdAt: Instant,
        lastCheckIn: Instant?
    ): Boolean {
        if (consumed) return false

        val cutoff = Instant.now().minus(abandonedAfterMinutes, ChronoUnit.MINUTES)

        val reference = lastCheckIn ?: createdAt
        return reference.isBefore(cutoff)
    }


    fun isStalled(task: PersistedTask): Boolean {
        if (task.consumed) return false

        val cutoff = stalledCutoffFor(task.task)
        return task.lastCheckIn?.isBefore(cutoff) ?: false
    }


    private fun stalledCutoffFor(taskName: String): Instant {
        return when (taskName) {
            MediaReadTask::class.getName() -> Instant.now().minus(5, ChronoUnit.MINUTES)
            EncodeTask::class.getName() -> Instant.now().minus(6, ChronoUnit.HOURS)
            ExtractSubtitleTask::class.getName() -> Instant.now().minus(15, ChronoUnit.MINUTES)
            ConvertTask::class.getName() -> Instant.now().minus(6, ChronoUnit.MINUTES)
            MetadataSearchTask::class.getName() -> Instant.now().minus(10, ChronoUnit.MINUTES)
            MigrateToContentStoreTask::class.getName() -> Instant.now().minus(30, ChronoUnit.MINUTES)
            StoreContentAndMetadataTask::class.getName() -> Instant.now().minus(5, ChronoUnit.MINUTES)
            else -> Instant.now().minus(30, ChronoUnit.MINUTES)
        }
    }

}
