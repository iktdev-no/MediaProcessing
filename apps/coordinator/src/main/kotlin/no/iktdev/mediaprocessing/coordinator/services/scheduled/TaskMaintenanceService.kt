package no.iktdev.mediaprocessing.coordinator.services.scheduled

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.UtcNow
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class TaskMaintenanceService {
    private val log = KotlinLogging.logger {}

    @Scheduled(fixedDelay = 10 * 60 * 1000)
    fun releaseAbandonedTasks() {
        val abandoned = TaskStore.findAbandonedTasks()
        abandoned.forEach { task ->
            val released = TaskStore.releaseExpiredTask(task.taskId)
            if (released) {
                val age = Duration.between(task.lastCheckIn, UtcNow())
                log.info("Released abandoned task ${task.taskId} (${task.task}) claimed by ${task.claimedBy}, last heartbeat $age ago")
            } else {
                log.warn("Failed to release abandoned task ${task.taskId} (${task.task}) claimed by ${task.claimedBy}")
            }
        }

    }

}