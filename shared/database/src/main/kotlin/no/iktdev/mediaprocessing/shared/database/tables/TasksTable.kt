package no.iktdev.mediaprocessing.shared.database.tables

import no.iktdev.eventi.models.store.TaskStatus
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object TasksTable: IntIdTable(name = "TASKS") {
    val referenceId: Column<String> = varchar("REFERENCE_ID", 36)
    val taskId: Column<String> = varchar("TASK_ID", 36)
    val task: Column<String> = varchar("TASK",100)
    val status: Column<TaskStatus> = enumerationByName("STATUS", 50, TaskStatus::class).default(TaskStatus.Pending)
    val data: Column<String> = text("DATA")
    val claimed: Column<Boolean> = bool("CLAIMED").default(false)
    val claimedBy: Column<String?> = varchar("CLAIMED_BY",100).nullable()
    val consumed: Column<Boolean> = bool("CONSUMED").default(false)
    val lastCheckIn: Column<LocalDateTime?> = datetime("LAST_CHECK_IN").nullable()
    val persistedAt: Column<LocalDateTime> = datetime("PERSISTED_AT").defaultExpression(CurrentDateTime)
}