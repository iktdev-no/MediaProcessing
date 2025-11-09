package no.iktdev.mediaprocessing.shared.common.stores

import com.google.gson.Gson
import no.iktdev.eventi.ZDS
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.stores.TaskStore
import no.iktdev.mediaprocessing.shared.common.database.tables.TasksTable
import no.iktdev.mediaprocessing.shared.common.database.withTransaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

object TaskStore: TaskStore {
    override fun persist(task: Task) {
        val asData = ZDS.WGson.toJson(task)
        val taskName = task::class.simpleName ?: run {
            throw RuntimeException("Missing class name for task: $task")
        }
        withTransaction {
            TasksTable.insert {
                it[referenceId] = task.referenceId
                it[taskId] = task.taskId
                it[TasksTable.task] = taskName
                it[status] = TaskStatus.Pending
                it[data] = asData
                it[persistedAt] = LocalDateTime.now()
            }
        }
    }

    override fun findByTaskId(taskId: UUID): PersistedTask? {
        return withTransaction {
            TasksTable.selectAll()
                .where { TasksTable.taskId eq taskId }
                .singleOrNull()?.let {
                    PersistedTask(
                        id = it[TasksTable.id].value.toLong(),
                        referenceId = it[TasksTable.referenceId],
                        status = it[TasksTable.status],
                        taskId = it[TasksTable.taskId],
                        task = it[TasksTable.task],
                        data = it[TasksTable.data],
                        claimed = it[TasksTable.claimed],
                        claimedBy = it[TasksTable.claimedBy],
                        consumed = it[TasksTable.consumed],
                        lastCheckIn = it[TasksTable.lastCheckIn],
                        persistedAt = it[TasksTable.persistedAt]
                    )
                }
        }.getOrNull()
    }

    override fun findByReferenceId(referenceId: UUID): List<PersistedTask> {
        return withTransaction {
            TasksTable.selectAll()
                .where { TasksTable.referenceId eq referenceId }
                .map {
                    PersistedTask(
                        id = it[TasksTable.id].value.toLong(),
                        referenceId = it[TasksTable.referenceId],
                        status = it[TasksTable.status],
                        taskId = it[TasksTable.taskId],
                        task = it[TasksTable.task],
                        data = it[TasksTable.data],
                        claimed = it[TasksTable.claimed],
                        claimedBy = it[TasksTable.claimedBy],
                        consumed = it[TasksTable.consumed],
                        lastCheckIn = it[TasksTable.lastCheckIn],
                        persistedAt = it[TasksTable.persistedAt]
                    )
                }
        }.getOrDefault(emptyList())
    }

    override fun findUnclaimed(referenceId: UUID): List<PersistedTask> {
        return withTransaction {
            TasksTable.selectAll()
                .where { (TasksTable.referenceId eq referenceId) and (TasksTable.claimed eq false) and (TasksTable.consumed eq false) }
                .map {
                    PersistedTask(
                        id = it[TasksTable.id].value.toLong(),
                        referenceId = it[TasksTable.referenceId],
                        status = it[TasksTable.status],
                        taskId = it[TasksTable.taskId],
                        task = it[TasksTable.task],
                        data = it[TasksTable.data],
                        claimed = it[TasksTable.claimed],
                        claimedBy = it[TasksTable.claimedBy],
                        consumed = it[TasksTable.consumed],
                        lastCheckIn = it[TasksTable.lastCheckIn],
                        persistedAt = it[TasksTable.persistedAt]
                    )
                }
        }.getOrDefault(emptyList())
    }

    override fun claim(taskId: UUID, workerId: String): Boolean {
        return withTransaction {
            TasksTable.update({
                (TasksTable.taskId eq taskId) and
                (TasksTable.claimed eq false)
            }) {
                it[claimed] = true
                it[claimedBy] = workerId
                it[lastCheckIn] = LocalDateTime.now()
            }
        }.isSuccess
    }

    override fun heartbeat(taskId: UUID) {
        withTransaction {
            TasksTable.update({ TasksTable.taskId eq taskId }) {
                it[lastCheckIn] = LocalDateTime.now()
            }
        }
    }

    override fun markConsumed(taskId: UUID) {
        withTransaction {
            TasksTable.update({ TasksTable.taskId eq taskId }) {
                it[consumed] = true
            }
        }
    }

    override fun releaseExpiredTasks(timeout: Duration) {
        val now = LocalDateTime.now()
        val expirationTime = now.minus(timeout)
        withTransaction {
            TasksTable.update({
                (TasksTable.claimed eq true) and
                (TasksTable.consumed eq false) and
                (TasksTable.lastCheckIn.isNotNull()) and
                (TasksTable.lastCheckIn less expirationTime)
            }) {
                it[claimed] = false
                it[claimedBy] = null
                it[lastCheckIn] = null
            }
        }
    }

    override fun getPendingTasks(): List<PersistedTask> {
        return withTransaction {
            TasksTable.selectAll()
                .where { (TasksTable.consumed eq false) and (TasksTable.claimed eq false) }
                .map {
                    PersistedTask(
                        id = it[TasksTable.id].value.toLong(),
                        referenceId = it[TasksTable.referenceId],
                        status = it[TasksTable.status],
                        taskId = it[TasksTable.taskId],
                        task = it[TasksTable.task],
                        data = it[TasksTable.data],
                        claimed = it[TasksTable.claimed],
                        claimedBy = it[TasksTable.claimedBy],
                        consumed = it[TasksTable.consumed],
                        lastCheckIn = it[TasksTable.lastCheckIn],
                        persistedAt = it[TasksTable.persistedAt]
                    )
                }
        }.getOrDefault(emptyList())
    }
}