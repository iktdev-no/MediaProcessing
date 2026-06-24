package no.iktdev.mediaprocessing.shared.database

import no.iktdev.eventi.MyTime
import no.iktdev.eventi.models.StoreResult
import no.iktdev.eventi.serialization.ZDS.toPersisted
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.stores.TaskStore
import no.iktdev.eventi.tasks.GlobalTaskPolicy
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.toJavaDuration

open class InMemoryTaskStore : TaskStore {
    private val tasks = mutableListOf<PersistedTask>()
    private var nextId = 1L

    fun wipe() {
        tasks.clear()
        nextId = 1L
    }

    override fun persist(task: Task): Boolean {
        val persistedTask = task.toPersisted(nextId++)
        tasks += persistedTask!!
        return true
    }

    override fun findByTaskId(taskId: UUID) = tasks.find { it.taskId == taskId }

    override fun findByReferenceId(referenceId: UUID) =
        tasks.filter { it.referenceId == referenceId }

    override fun findUnclaimed(referenceId: UUID) =
        tasks.filter { it.referenceId == referenceId && !it.claimed && !it.consumed }

    override fun claim(taskId: UUID, workerId: String): Boolean {
        val task = findByTaskId(taskId) ?: return false
        if (task.claimed && !isExpired(task)) return false
        update(task.copy(claimed = true, claimedBy = workerId, lastCheckIn = MyTime.utcNow()))
        return true
    }

    override fun heartbeat(taskId: UUID): Boolean {
        val task = findByTaskId(taskId) ?: return false
        update(task.copy(lastCheckIn = MyTime.utcNow()))
        return true
    }

    override fun markConsumed(taskId: UUID, status: TaskStatus): Boolean {
        val task = findByTaskId(taskId) ?: return false
        update(task.copy(consumed = true, status = status))
        return true
    }

    override fun releaseExpiredTasks() {
        val now = MyTime.utcNow().minus(GlobalTaskPolicy.policy.abandonTimeout().toJavaDuration())
        tasks.filter {
            it.claimed && !it.consumed && it.lastCheckIn?.isBefore(now) == true
        }.forEach {
            update(it.copy(claimed = false, claimedBy = null, lastCheckIn = null))
        }
    }

    override fun resetTaskById(taskId: UUID): Boolean {
        val state = tasks.find { it.taskId == taskId }?.let {
            update(it.copy(claimed = false, lastCheckIn = null, status = TaskStatus.Pending, consumed = false))
            true
        } ?: false
        return state
    }

    override fun resetTasksById(taskId: List<UUID>): StoreResult {
        val out = tasks.filter { it.taskId in taskId && it.consumed && it.status != TaskStatus.Pending }.onEach {
            update(it.copy(claimed = false, lastCheckIn = null, status = TaskStatus.Pending, consumed = false))
        }
        return StoreResult(out.size == taskId.size, out.size)
    }

    override fun deleteTasksById(taskId: UUID): StoreResult {
        val success = tasks.find { it.taskId == taskId }?.let {
            tasks.remove(it)
            1
        } ?: 0
        return StoreResult(success == 1, success)
    }

    override fun getPendingTasks() = tasks.filter { !it.consumed }

    private fun update(updated: PersistedTask) {
        tasks.replaceAll { if (it.taskId == updated.taskId) updated else it }
    }

    private fun isExpired(task: PersistedTask): Boolean {
        val now = MyTime.utcNow()
        return task.lastCheckIn?.isBefore(now.minus(15, ChronoUnit.MINUTES)) == true
    }

    private fun serialize(data: Any?): String = data?.toString() ?: "{}"

    fun clear() { tasks.clear(); nextId = 1L }

}
