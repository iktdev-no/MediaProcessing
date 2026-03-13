package no.iktdev.mediaprocessing.shared.database.stores

import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.serialization.WGson
import no.iktdev.eventi.stores.TaskStore
import no.iktdev.eventi.tasks.GlobalTaskPolicy
import no.iktdev.mediaprocessing.shared.common.UtcNow
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.dto.TaskQuery
import no.iktdev.mediaprocessing.shared.database.likeAny
import no.iktdev.mediaprocessing.shared.database.queries.pagedQuery
import no.iktdev.mediaprocessing.shared.database.tables.TasksTable
import no.iktdev.mediaprocessing.shared.database.withTransaction
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.time.Duration
import kotlin.time.toJavaDuration

object TaskStore: TaskStore {

    fun getPagedTasks(query: TaskQuery): Paginated<PersistedTask> =
        pagedQuery(
            table = TasksTable,
            query = query,
            sortColumns = mapOf(
                "taskId" to TasksTable.taskId,
                "referenceId" to TasksTable.referenceId,
                "status" to TasksTable.status,
                "persistedAt" to TasksTable.persistedAt,
                "lastCheckIn" to TasksTable.lastCheckIn
            ),
            applyFilters = {
                query.status?.let { statuses ->
                    val enums = statuses.map { TaskStatus.valueOf(it) }
                    where { TasksTable.status inList enums }
                }
                query.key?.let { keys ->
                    where { TasksTable.task.likeAny(keys) }
                }

                query.claimed?.let { where { TasksTable.claimed eq it } }
                query.consumed?.let { where { TasksTable.consumed eq it } }
                query.referenceId?.let { where { TasksTable.referenceId like "%$it%" } }
                query.from?.let { where { TasksTable.persistedAt greaterEq it } }
                query.to?.let { where { TasksTable.persistedAt lessEq it } }
            },
            mapper = { row ->
                PersistedTask(
                    id = row[TasksTable.id].value.toLong(),
                    referenceId = UUID.fromString(row[TasksTable.referenceId]),
                    status = row[TasksTable.status],
                    taskId = UUID.fromString(row[TasksTable.taskId]),
                    task = row[TasksTable.task],
                    data = row[TasksTable.data],
                    claimed = row[TasksTable.claimed],
                    claimedBy = row[TasksTable.claimedBy],
                    consumed = row[TasksTable.consumed],
                    lastCheckIn = row[TasksTable.lastCheckIn],
                    persistedAt = row[TasksTable.persistedAt]
                )
            }
        )



    override fun persist(task: Task): Boolean {
        val asData = WGson.toJson(task)
        val taskName = task::class.simpleName ?: run {
            throw RuntimeException("Missing class name for task: $task")
        }
        return withTransaction {
            TasksTable.insert {
                it[referenceId] = task.referenceId.toString()
                it[taskId] = task.taskId.toString()
                it[TasksTable.task] = taskName
                it[status] = TaskStatus.Pending
                it[data] = asData
                it[persistedAt] = UtcNow()
            }
        }.isSuccess
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
            TasksTable.getWhere {
                TasksTable.referenceId eq referenceId.toString()
            }
        }.getOrDefault(emptyList())
    }

    override fun findUnclaimed(referenceId: UUID): List<PersistedTask> {
        return withTransaction {
            TasksTable.getWhere {
                (TasksTable.referenceId eq referenceId.toString()) and
                        (TasksTable.claimed eq false) and
                        (TasksTable.consumed eq false)
            }
        }.getOrDefault(emptyList())
    }

    fun findActiveTasks(): List<PersistedTask> {
        return withTransaction {
            TasksTable.getWhere {
                (TasksTable.status inList listOf(TaskStatus.Pending, TaskStatus.InProgress)) and
                        (TasksTable.consumed eq false)
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
                it[status] = TaskStatus.InProgress
            }
        }.isSuccess
    }

    override fun heartbeat(taskId: UUID): Boolean {
        return withTransaction {
            TasksTable.update({ TasksTable.taskId eq taskId.toString() }) {
                it[lastCheckIn] = UtcNow()
            }
        }.isSuccess
    }

    override fun markConsumed(taskId: UUID, status: TaskStatus): Boolean {
        return withTransaction {
            TasksTable.update({ TasksTable.taskId eq taskId.toString() }) {
                it[consumed] = true
                it[TasksTable.status] = status
                it[lastCheckIn] = UtcNow()
            }
        }.isSuccess
    }


    override fun releaseExpiredTasks() {
        val expirationTime = GlobalTaskPolicy.policy.abandonTimeout().ago()
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

    fun releaseExpiredTask(taskId: UUID): Boolean {
        val expirationTime = GlobalTaskPolicy.policy.abandonTimeout().ago()
        return withTransaction {
            TasksTable.update({
                (TasksTable.claimed eq true) and
                        (TasksTable.consumed eq false) and
                        (TasksTable.lastCheckIn.isNotNull()) and
                        (TasksTable.lastCheckIn less expirationTime) and
                        (TasksTable.taskId eq taskId.toString())
            }) {
                it[claimed] = false
                it[claimedBy] = null
                it[lastCheckIn] = null
            }
        }.isSuccess
    }

    fun resetTaskById(taskId: UUID): Result<Int> {
        return withTransaction {
            TasksTable.update({
                (TasksTable.claimed eq true) and
                        (TasksTable.consumed eq true) and
                        (TasksTable.status eq TaskStatus.Failed) and
                        (TasksTable.taskId eq taskId.toString())
            }) {
                it[claimed] = false
                it[claimedBy] = null
                it[consumed] = false
                it[lastCheckIn] = null
                it[status] = TaskStatus.Pending
            }
        }
    }

    override fun getPendingTasks(): List<PersistedTask> {
        return withTransaction {
            TasksTable.getWhere {
                (TasksTable.consumed eq false) and
                        (TasksTable.claimed eq false) and
                        (TasksTable.status eq TaskStatus.Pending)
            }
        }.getOrDefault(emptyList())
    }

    fun findAbandonedTasks(): List<PersistedTask> {
        val cutoff = GlobalTaskPolicy.policy.abandonTimeout().ago()
        return withTransaction {
            TasksTable.getWhere {
                (TasksTable.lastCheckIn less cutoff or TasksTable.lastCheckIn.isNull()) and
                        (TasksTable.consumed eq false) and
                        (TasksTable.claimed eq true)
            }
        }.getOrDefault(emptyList())
    }

    fun getFailedTasks(): List<PersistedTask> {
        return withTransaction {
            TasksTable.getWhere {
                (TasksTable.status eq TaskStatus.Failed)
            }
        }.getOrDefault(emptyList())
    }

    fun Duration.ago(): Instant = UtcNow().minus(this.toJavaDuration())

}