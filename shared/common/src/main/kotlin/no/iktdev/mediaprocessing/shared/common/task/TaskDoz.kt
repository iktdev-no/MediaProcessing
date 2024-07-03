package no.iktdev.mediaprocessing.shared.common.task

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mysql.cj.xdevapi.RowResult
import no.iktdev.mediaprocessing.shared.common.persistance.tasks
import org.jetbrains.exposed.sql.ResultRow

class TaskDoz {
    val gson = Gson()

    fun <T: TaskData> dzdata(type: TaskType, data: String): T? {
        val clazz: Class<out TaskData> = when(type) {
            TaskType.Encode, TaskType.Extract -> {
                FfmpegTaskData::class.java
            }
            TaskType.Convert -> {
                ConvertTaskData::class.java
            }
            else -> TaskData::class.java
        }
        val type = TypeToken.getParameterized(clazz).type
        return gson.fromJson(data, type)
    }

    fun deserializeTask(row: ResultRow): Task? {
        val taskType = row[tasks.task].let { TaskType.valueOf(it) }
        val data = row[tasks.data]

        return Task(
            referenceId = row[tasks.referenceId],
            status = row[tasks.status],
            claimed = row[tasks.claimed],
            claimedBy = row[tasks.claimedBy],
            consumed = row[tasks.consumed],
            task = taskType,
            eventId = row[tasks.eventId],
            derivedFromEventId = row[tasks.derivedFromEventId],
            data = dzdata(taskType, data),
            created = row[tasks.created],
            lastCheckIn = row[tasks.lastCheckIn]
        )
    }
}