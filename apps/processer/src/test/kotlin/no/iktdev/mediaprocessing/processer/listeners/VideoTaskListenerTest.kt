package no.iktdev.mediaprocessing.processer.listeners

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.processer.TestBase
import no.iktdev.mediaprocessing.processer.TestUtils
import no.iktdev.mediaprocessing.processer.config.ProcesserProperties
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.LinearEncodeTask
import no.iktdev.mediaprocessing.shared.common.model.task.data.LinearEncodeData
import org.junit.jupiter.api.Assertions.*
import java.util.UUID

class VideoTaskListenerTest: TestBase() {

    private val props = ProcesserProperties(
        coordinatorUrl = "http://localhost",
        coordinatorPingOnStartup = false,
        allowOverwrite = true,
        enableSegmentedTaskListener = true
    )


    private val listener = object : VideoTaskListener(TaskType.CPU_INTENSIVE, mockExecConfig) {
        override fun getWorkerId(): String {
            return UUID.randomUUID().toString()
        }

        override fun supports(task: Task): Boolean {
            return true
        }

        override suspend fun onTask(task: Task): Event? {
            return null
        }
    }

}
