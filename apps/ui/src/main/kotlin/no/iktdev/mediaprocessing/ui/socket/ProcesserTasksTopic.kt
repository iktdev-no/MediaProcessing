package no.iktdev.mediaprocessing.ui.socket

import no.iktdev.eventi.data.referenceId
import no.iktdev.eventi.database.toEpochSeconds
import no.iktdev.eventi.database.withDirtyRead
import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.ProcessType
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.dto.OperationEvents
import no.iktdev.mediaprocessing.shared.common.contract.dto.ProcesserEventInfo
import no.iktdev.mediaprocessing.shared.common.database.cal.toEvent
import no.iktdev.mediaprocessing.shared.common.database.cal.toTask
import no.iktdev.mediaprocessing.shared.common.database.tables.events
import no.iktdev.mediaprocessing.shared.common.database.tables.tasks
import no.iktdev.mediaprocessing.shared.common.task.Task
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.ui.WebSocketMonitoringService
import no.iktdev.mediaprocessing.ui.eventDatabase
import no.iktdev.mediaprocessing.ui.socket.a2a.ProcesserListenerService
import org.jetbrains.exposed.sql.selectAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.io.File

@Service
class ProcesserTasksTopic(
    @Autowired a2AProcesserService: ProcesserListenerService,
    @Autowired private val webSocketMonitoringService: WebSocketMonitoringService,
    @Autowired private val message: SimpMessagingTemplate?,
): SocketListener(message) {


    final val a2a = object : ProcesserListenerService.A2AProcesserListener {
        override fun onExtractProgress(info: ProcesserEventInfo) {
            message?.convertAndSend("/topic/processer/extract/progress", info)
            pullAllTasks()
        }

        override fun onEncodeProgress(info: ProcesserEventInfo) {
            message?.convertAndSend("/topic/processer/encode/progress", info)
            pullAllTasks()
        }

        override fun onEncodeAssigned() {
            pullAllTasks()
        }

        override fun onExtractAssigned() {
            pullAllTasks()
        }
    }

    init {
        a2AProcesserService.attachListener(a2a)
    }

    enum class Status {
        Skipped,
        Awaiting, // Waiting for tasks to be created
        NeedsApproval,
        Pending,
        InProgress,
        Completed,
        Failed,
    }

    data class ContentEventState(
        val referenceId: String,
        val title: String,
        val encode: Status = Status.Skipped,
        val extract: Status = Status.Skipped,
        val convert: Status = Status.Skipped,
        val created: Long
    ) {}


    @MessageMapping("/tasks/all")
    fun pullAllTaskss() {
        val states = update()
        template?.convertAndSend("/topic/tasks/all", states)
    }

    fun getOperationState(tasks: List<Task>, hasOperation: Boolean, canStart: Boolean): Status {
        if (!hasOperation) return Status.Skipped
        if (tasks.isEmpty()) return Status.Awaiting

        if (!canStart) return Status.NeedsApproval

        if (tasks.any { it.consumed }) {
            return Status.Completed
        }
        if (tasks.any { it.claimed }) {
            return Status.InProgress
        }
        if (tasks.any{ it.status == "ERROR"}) {
            return Status.Failed
        }
        return Status.Pending

    }


    fun update(): MutableList<ContentEventState> {
        val eventStates: MutableList<ContentEventState> = mutableListOf()

        val tasks = pullAllTasks()
        val availableEvents = pullAllEvents()

        for ((referenceId, events) in availableEvents) {
            val startEvent = events.findFirstEventOf<MediaProcessStartEvent>() ?: continue
            val startData = startEvent.data ?: continue
            val title = events.findFirstEventOf<BaseInfoEvent>()?.data?.sanitizedName ?: startData.file.let { File(it).nameWithoutExtension }
            val canStart = if (startData.type == ProcessType.FLOW) true else {
                events.findEventsOf<PermitWorkCreationEvent>().isNotEmpty()
            }
            val tasksCreated = tasks[referenceId]
            val encode = tasksCreated?.filter { it.task == TaskType.Encode } ?: emptyList()
            val extract = tasksCreated?.filter { it.task == TaskType.Extract } ?: emptyList()
            val convert = tasksCreated?.filter { it.task == TaskType.Convert } ?: emptyList()

            eventStates.add(ContentEventState(
                title = title,
                referenceId = referenceId,
                encode = getOperationState(encode, startData.operations.contains(OperationEvents.ENCODE), canStart),
                extract = getOperationState(extract, startData.operations.contains(OperationEvents.EXTRACT), canStart),
                convert = getOperationState(convert, startData.operations.contains(OperationEvents.CONVERT), canStart),
                created = startEvent.metadata.created.toEpochSeconds() * 1000L
            ))
        }
        return eventStates
    }

    fun pullAllTasks(): Map<String, List<Task>> {
        val result = withTransaction(eventDatabase.database) {
            tasks.selectAll().toTask()
                .groupBy { it.referenceId }
        } ?: emptyMap()
        return result
    }

    fun pullAllEvents(): Map<String, List<Event>> {
        val result = withDirtyRead(eventDatabase.database) {
            events.selectAll().toEvent()
                .groupBy { it.referenceId() }
        } ?: emptyMap()
        return result
    }

}