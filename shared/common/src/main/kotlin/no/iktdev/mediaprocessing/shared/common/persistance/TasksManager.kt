package no.iktdev.mediaprocessing.shared.common.persistance

import mu.KotlinLogging
import no.iktdev.eventi.data.eventId
import no.iktdev.eventi.data.referenceId
import no.iktdev.eventi.data.toJson
import no.iktdev.mediaprocessing.shared.common.datasource.*
import no.iktdev.mediaprocessing.shared.common.task.Task
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.common.task.TaskDoz
import no.iktdev.mediaprocessing.shared.contract.data.Event
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*

enum class Status {
    SKIPPED,
    COMPLETED,
    ERROR
}

class TasksManager(private val dataSource: DataSource) {
    private val log = KotlinLogging.logger {}

    fun getClaimableTasks(): Map<TaskType, List<Task>> {
        return withTransaction(dataSource.database) {
            tasks.select {
                (tasks.consumed eq false) and
                        (tasks.claimed eq false)
            }.toTaskTypeGroup()
        } ?: emptyMap()
    }

    fun getTasksFor(referenceId: String): List<Task> {
        return withDirtyRead(dataSource) {
            tasks.select {
                (tasks.referenceId eq referenceId)
            }.toTask()
        } ?: emptyList()
    }

    fun getTaskWith(referenceId: String, eventId: String): Task? {
        return withDirtyRead(dataSource.database) {
            tasks.select {
                (tasks.referenceId eq referenceId) and
                        (tasks.eventId eq eventId)
            }.toTask()
        }?.singleOrNull()
    }

    fun getTasksWithExpiredClaim(): List<Task> {
        val deadline = LocalDateTime.now()
        return getUncompletedTasks()
            .filter { it.claimed && if (it.lastCheckIn != null) it.lastCheckIn.plusMinutes(15) < deadline else true }
    }

    fun isTaskClaimed(referenceId: String, eventId: String): Boolean {
        val info = getTaskWith(referenceId, eventId)
        return info?.claimed ?: true && info?.consumed ?: true
    }

    fun isTaskCompleted(referenceId: String, eventId: String): Boolean {
        return getTaskWith(referenceId, eventId)?.consumed ?: false
    }

    fun getUncompletedTasks(): List<Task> {
        return withTransaction(dataSource.database) {
            tasks.select {
                (tasks.consumed eq false)
            }.toTask()
        } ?: emptyList()
    }


    fun markTaskAsClaimed(referenceId: String, eventId: String, claimer: String): Boolean {
        return executeWithStatus(dataSource.database) {
            tasks.update({
                (tasks.referenceId eq referenceId) and
                        (tasks.eventId eq eventId) and
                        (tasks.claimed eq false) and
                        (tasks.consumed eq false)
            }) {
                it[claimedBy] = claimer
                it[lastCheckIn] = CurrentDateTime
                it[claimed] = true
            }
        }
    }

    fun markTaskAsCompleted(referenceId: String, eventId: String, status: Status = Status.COMPLETED): Boolean {
        return executeWithStatus(dataSource) {
            tasks.update({
                (tasks.referenceId eq referenceId) and
                        (tasks.eventId eq eventId)
            }) {
                it[consumed] = true
                it[claimed] = true
                it[tasks.status] = status.name
            }
        }
    }

    fun refreshTaskClaim(referenceId: String, eventId: String, claimer: String): Boolean {
        return executeWithStatus(dataSource) {
            tasks.update({
                (tasks.referenceId eq referenceId) and
                        (tasks.eventId eq eventId) and
                        (tasks.claimed eq true) and
                        (tasks.claimedBy eq claimer)
            }) {
                it[lastCheckIn] = CurrentDateTime
            }
        }
    }

    fun deleteTaskClaim(referenceId: String, eventId: String): Boolean {
        return executeWithStatus(dataSource) {
            tasks.update({
                (tasks.referenceId eq referenceId) and
                        (tasks.eventId eq eventId)
            }) {
                it[claimed] = false
                it[claimedBy] = null
                it[lastCheckIn] = null
            }
        }
    }

    fun createTask(referenceId: String, eventId: String = UUID.randomUUID().toString(), derivedFromEventId: String? = null, task: TaskType, data: String, inputFile: String): Boolean {
        return executeWithStatus(dataSource) {
            tasks.insert {
                it[tasks.referenceId] = referenceId
                it[tasks.eventId] = eventId
                it[tasks.task] = task.name
                it[tasks.data] = data
                it[tasks.derivedFromEventId] = derivedFromEventId
                it[tasks.inputFile] = inputFile
            }
        }
    }


    fun produceEvent(event: Event): Boolean {
        val exception = executeOrException(dataSource.database) {
            events.insert {
                it[referenceId] = event.referenceId()
                it[eventId] = event.eventId()
                it[events.event] = event.eventType.event
                it[data] = event.toJson()
            }
        }
        val success = if (exception != null) {
            if (exception.isExposedSqlException()) {
                if ((exception as ExposedSQLException).isCausedByDuplicateError()) {
                    log.debug { "Error is of SQLIntegrityConstraintViolationException" }
                    log.error { exception.message }
                    exception.printStackTrace()
                } else {
                    log.debug { "Error code is: ${exception.errorCode}" }
                    log.error { exception.message }
                    exception.printStackTrace()
                }
            } else {
                log.error { exception.message }
                exception.printStackTrace()
            }
            false
        } else {
            true
        }
        return success
    }

}

val digest = MessageDigest.getInstance("MD5")
@OptIn(ExperimentalStdlibApi::class)
private fun getIntegrityOfData(data : String) : String {
    return digest.digest(data.toByteArray(kotlin.text.Charsets.UTF_8))
        .toHexString()
}

fun Query?.toTaskTypeGroup(): Map<TaskType, List<Task>> {
    val dz = TaskDoz()
    val res = this?.mapNotNull {  dz.deserializeTask(it) }?.groupBy { it.task } ?: emptyMap()
    return res
}

fun Query?.toTask(): List<Task> {
    val dz = TaskDoz()
    val res = this?.mapNotNull {  dz.deserializeTask(it) } ?: emptyList()
    return res
}