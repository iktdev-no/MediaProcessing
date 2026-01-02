package no.iktdev.mediaprocessing.converter.listeners

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.converter.convert.ConvertListener
import no.iktdev.mediaprocessing.converter.convert.Converter2
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ConvertTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import org.springframework.stereotype.Component
import java.util.*

@Component
class ConvertTaskListener: TaskListener(TaskType.CPU_INTENSIVE) {

    override fun getWorkerId(): String {
        return "${this::class.java.simpleName}-${TaskType.CPU_INTENSIVE}-${UUID.randomUUID()}"
    }

    override fun supports(task: Task): Boolean {
        return task is ConvertTask
    }

    override suspend fun onTask(task: Task): Event? {
        if (task !is ConvertTask) {
            throw IllegalArgumentException("Invalid task type: ${task::class.java.name}")
        }
        val converter = Converter2(task.data, object: ConvertListener {
            override fun onStarted(inputFile: String) {
            }
            override fun onCompleted(inputFile: String, outputFiles: List<String>) {
            }
        })

        withHeartbeatRunner {
            reporter?.updateLastSeen(task.taskId)
        }

        converter.execute()

        return try {
            val result = converter.getResult()
            val newEvent = ConvertTaskResultEvent(
                data = ConvertTaskResultEvent.ConvertedData(
                    language = task.data.language,
                    outputFiles = result,
                    baseName = task.data.outputFileName
                ),
                status = TaskStatus.Completed
            ).producedFrom(task)
            newEvent
        } catch (e: Exception) {
            e.printStackTrace()
            val newEvent = ConvertTaskResultEvent(
                data = null,
                status = TaskStatus.Failed
            ).producedFrom(task)
            newEvent
        }


    }


}