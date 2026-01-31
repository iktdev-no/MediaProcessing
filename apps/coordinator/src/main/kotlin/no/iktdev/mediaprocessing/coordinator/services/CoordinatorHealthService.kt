package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.coordinator.dto.health.CoordinatorHealth
import no.iktdev.mediaprocessing.coordinator.dto.health.CoordinatorHealthStatus
import no.iktdev.mediaprocessing.coordinator.dto.health.SequenceHealth
import no.iktdev.mediaprocessing.coordinator.dto.rate.EventRate
import no.iktdev.mediaprocessing.coordinator.util.DiskInfo
import no.iktdev.mediaprocessing.coordinator.util.getDiskInfoFor
import no.iktdev.mediaprocessing.shared.common.dto.CurrentState
import no.iktdev.mediaprocessing.shared.common.rules.EventLifecycleRules
import no.iktdev.mediaprocessing.shared.common.rules.TaskLifecycleRules
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class CoordinatorHealthService(
    private val taskService: TaskService,
    private val eventService: EventService,
    private val aggregator: SequenceAggregatorService,
    private val coordinatorEnv: CoordinatorEnv
) {

    fun getHealth(): CoordinatorHealth {
        val tasks = taskService.getActiveTasks()
        val incompleteSequences = eventService.getIncompleteSequences()
            .groupBy { it.referenceId }
            .values

        // --- TASK HEALTH ---
        val abandonedTaskIds = tasks
            .filter { TaskLifecycleRules.isAbandoned(it.consumed, it.lastCheckIn) }
            .map { it.taskId }

        val stalledTaskIds = tasks
            .filter { TaskLifecycleRules.isStalled(it) }
            .map { it.taskId }

        val failedTasks = taskService.getFailedTasks()
        val sequencesOnHold = aggregator.getActiveSequences().filter { it.currentState == CurrentState.OnHold }

        // --- SEQUENCE HEALTH ---
        val overdueSequences = incompleteSequences
            .filter { EventLifecycleRules.isOverdue(it) }
            .map { seq ->
                val refId = seq.first().referenceId
                val firstEventAt = seq.minOf { it.persistedAt }
                val lastEventAt = seq.maxOf { it.persistedAt }

                val expectedWindow = EventLifecycleRules.expectedCompletionTimeWindow(seq)
                val actualAge = Duration.between(firstEventAt, Instant.now())

                // Operasjonelle verdier frontend trenger
                val expectedFinish = firstEventAt.plus(expectedWindow)
                val overdueDuration = actualAge.minus(expectedWindow).coerceAtLeast(Duration.ZERO)
                val isOverdue = overdueDuration > Duration.ZERO

                SequenceHealth(
                    referenceId = refId.toString(),

                    // eksisterende felter
                    age = actualAge,
                    expected = expectedWindow,
                    lastEventAt = lastEventAt,
                    eventCount = seq.size,

                    // nye operasjonelle felter
                    startTime = firstEventAt,
                    expectedFinishTime = expectedFinish,
                    overdueDuration = overdueDuration,
                    isOverdue = isOverdue
                )
            }

        val overdueSequenceIds = overdueSequences.map { it.referenceId }

        // --- AGGREGATED STATUS ---
        val status = when {
            abandonedTaskIds.isNotEmpty() ||
                    stalledTaskIds.isNotEmpty() ||
                    overdueSequenceIds.isNotEmpty() -> CoordinatorHealthStatus.DEGRADED

            else -> CoordinatorHealthStatus.HEALTHY
        }


        val lastActivityCandidates = listOfNotNull(
            tasks.maxOfOrNull { it.persistedAt },
            eventService.getLastEventTimestamp()
        )

        return CoordinatorHealth(
            status = status,
            abandonedTasks = abandonedTaskIds.size,
            stalledTasks = stalledTaskIds.size,
            activeTasks = tasks.count { !it.consumed },
            failedTasks = failedTasks.count(),
            queuedTasks = TaskStore.getPendingTasks().size,
            lastActivity = lastActivityCandidates.maxOrNull(),

            abandonedTaskIds = abandonedTaskIds.map { it.toString() },
            stalledTaskIds = stalledTaskIds.map { it.toString() },
            sequencesOnHold = sequencesOnHold.size,
            sequencesOnHoldIds = sequencesOnHold.map { it.referenceId },
            overdueSequenceIds = overdueSequenceIds,
            overdueSequences = overdueSequences,

            details = mapOf(
                "oldestActiveTaskAgeMinutes" to tasks.minOfOrNull {
                    Duration.between(it.persistedAt, Instant.now()).toMinutes()
                }
            )
        )
    }

    fun getEventRate(): EventRate {
        return EventRate(
            lastMinute = eventService.getEventsLast(1),
            lastFiveMinutes = eventService.getEventsLast(5)
        )
    }

    fun getDiskHealth(): List<DiskInfo> {
        val paths = listOf(coordinatorEnv.incomingContent,
            coordinatorEnv.cachedContent,
            coordinatorEnv.outgoingContent)
            .map { it -> it.absolutePath }
        return getDiskInfoFor(paths)
    }
}
