package no.iktdev.mediaprocessing.shared.database.stores

import no.iktdev.eventi.ZDS
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.stores.TaskStore
import no.iktdev.mediaprocessing.shared.common.UtcNow
import no.iktdev.mediaprocessing.shared.common.dto.PagedTasks
import no.iktdev.mediaprocessing.shared.database.tables.TasksTable
import no.iktdev.mediaprocessing.shared.database.withTransaction
import org.jetbrains.exposed.sql.*
import java.time.Duration
import java.util.*

object TaskStore: TaskStore {

    fun getPagedTasks(page: Int, size: Int): PagedTasks {
        return withTransaction {
            val total = TasksTable.selectAll().count()
            val rows = TasksTable
                .selectAll()
                .orderBy(TasksTable.persistedAt, SortOrder.DESC)
                .limit(size).offset(start = (page * size).toLong())
                .map { it ->
                    PersistedTask(
                        id = it[TasksTable.id].value.toLong(),
                        referenceId = UUID.fromString(it[TasksTable.referenceId]),
                        status = it[TasksTable.status],
                        taskId = UUID.fromString(it[TasksTable.taskId]),
                        task = it[TasksTable.task],
                        data = it[TasksTable.data],
                        claimed = it[TasksTable.claimed],
                        claimedBy = it[TasksTable.claimedBy],
                        consumed = it[TasksTable.consumed],
                        lastCheckIn = it[TasksTable.lastCheckIn],
                        persistedAt = it[TasksTable.persistedAt]
                    )
                }
            PagedTasks(
                content = rows,
                page = page,
                size = size,
                total = total
            )
        }.getOrDefault(PagedTasks(emptyList(), page, size, 0))
    }


    override fun persist(task: Task) {
        val asData = ZDS.WGson.toJson(task)
        val taskName = task::class.simpleName ?: run {
            throw RuntimeException("Missing class name for task: $task")
        }
        withTransaction {
            TasksTable.insert {
                it[referenceId] = task.referenceId.toString()
                it[taskId] = task.taskId.toString()
                it[TasksTable.task] = taskName
                it[status] = TaskStatus.Pending
                it[data] = asData
                it[persistedAt] = UtcNow()
            }
        }
    }

    override fun findByTaskId(taskId: UUID): PersistedTask? {
        return withTransaction {
            TasksTable.selectAll()
                .where { TasksTable.taskId eq taskId.toString() }
                .singleOrNull()?.let {
                    PersistedTask(
                        id = it[TasksTable.id].value.toLong(),
                        referenceId = UUID.fromString(it[TasksTable.referenceId]),
                        status = it[TasksTable.status],
                        taskId = UUID.fromString(it[TasksTable.taskId]),
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
                .where { TasksTable.referenceId eq referenceId.toString() }
                .map {
                    PersistedTask(
                        id = it[TasksTable.id].value.toLong(),
                        referenceId = UUID.fromString(it[TasksTable.referenceId]),
                        status = it[TasksTable.status],
                        taskId = UUID.fromString(it[TasksTable.taskId]),
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
                .where { (TasksTable.referenceId eq referenceId.toString()) and (TasksTable.claimed eq false) and (TasksTable.consumed eq false) }
                .map {
                    PersistedTask(
                        id = it[TasksTable.id].value.toLong(),
                        referenceId = UUID.fromString(it[TasksTable.referenceId]),
                        status = it[TasksTable.status],
                        taskId = UUID.fromString(it[TasksTable.taskId]),
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

    fun findActiveTasks(): List<PersistedTask> {
        return withTransaction {
            TasksTable.selectAll()
                .where { (TasksTable.status inList listOf(TaskStatus.Pending, TaskStatus.InProgress)) and (TasksTable.consumed eq false) }
                .map {
                    PersistedTask(
                        id = it[TasksTable.id].value.toLong(),
                        referenceId = UUID.fromString(it[TasksTable.referenceId]),
                        status = it[TasksTable.status],
                        taskId = UUID.fromString(it[TasksTable.taskId]),
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
                (TasksTable.taskId eq taskId.toString()) and
                (TasksTable.claimed eq false)
            }) {
                it[claimed] = true
                it[claimedBy] = workerId
                it[lastCheckIn] = UtcNow()
            }
        }.isSuccess
    }

    override fun heartbeat(taskId: UUID) {
        withTransaction {
            TasksTable.update({ TasksTable.taskId eq taskId.toString() }) {
                it[lastCheckIn] = UtcNow()
            }
        }
    }

    override fun markConsumed(taskId: UUID, status: TaskStatus) {
        withTransaction {
            TasksTable.update({ TasksTable.taskId eq taskId.toString() }) {
                it[consumed] = true
                it[TasksTable.status] = status
            }
        }
    }

    override fun releaseExpiredTasks(timeout: Duration) {
        val now = UtcNow()
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
                        referenceId = UUID.fromString(it[TasksTable.referenceId]),
                        status = it[TasksTable.status],
                        taskId = UUID.fromString(it[TasksTable.taskId]),
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