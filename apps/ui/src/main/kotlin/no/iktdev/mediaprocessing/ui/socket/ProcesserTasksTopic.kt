package no.iktdev.mediaprocessing.ui.socket

import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.shared.common.contract.dto.ProcesserEventInfo
import no.iktdev.mediaprocessing.shared.common.database.cal.toTask
import no.iktdev.mediaprocessing.shared.common.database.tables.tasks
import no.iktdev.mediaprocessing.shared.common.task.Task
import no.iktdev.mediaprocessing.ui.WebSocketMonitoringService
import no.iktdev.mediaprocessing.ui.eventDatabase
import no.iktdev.mediaprocessing.ui.socket.a2a.ProcesserListenerService
import org.jetbrains.exposed.sql.selectAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class ProcesserTasksTopic(
    @Autowired a2AProcesserService: ProcesserListenerService,
    @Autowired private val webSocketMonitoringService: WebSocketMonitoringService,
    @Autowired override var template: SimpMessagingTemplate?,
): SocketListener(template) {

    final val a2a = object : ProcesserListenerService.A2AProcesserListener {
        override fun onExtractProgress(info: ProcesserEventInfo) {
        }

        override fun onEncodeProgress(info: ProcesserEventInfo) {
        }

        override fun onEncodeAssigned() {
        }

        override fun onExtractAssigned() {
        }
    }

    init {
        a2AProcesserService.attachListener(a2a)
    }

    data class TaskGroup(
        val referenceId: String,
        val tasks: List<Task>
    )

    @MessageMapping("/tasks/all")
    fun pullAllTasks() {
        val result = withTransaction(eventDatabase.database) {
            tasks.selectAll().toTask()
                .groupBy { it.referenceId }.map { g -> TaskGroup(g.key, g.value) }
        } ?: emptyList()
        template?.convertAndSend("/topic/tasks/all", result)
    }

}