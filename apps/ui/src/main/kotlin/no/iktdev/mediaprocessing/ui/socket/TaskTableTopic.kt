package no.iktdev.mediaprocessing.ui.socket

import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.shared.common.database.cal.toTask
import no.iktdev.mediaprocessing.shared.common.database.tables.tasks
import no.iktdev.mediaprocessing.shared.common.task.Task
import no.iktdev.mediaprocessing.ui.eventDatabase
import no.iktdev.mediaprocessing.ui.socket.impl.SocketListener
import org.jetbrains.exposed.sql.selectAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class TaskTableTopic(
    @Autowired private val message: SimpMessagingTemplate?,
) : SocketListener(message) {



    data class TaskGroup(
        val referenceId: String,
        val tasks: List<Task>
    )

    @MessageMapping("/taskTable/all")
    fun pullAllTasks() {
        val result = withTransaction(eventDatabase.database) {
            tasks.selectAll().toTask()
                .groupBy { it.referenceId }.map { g -> TaskGroup(g.key, g.value) }
        } ?: emptyList()
        template?.convertAndSend("/topic/taskTable/all", result)
    }
}