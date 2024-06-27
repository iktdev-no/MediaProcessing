package no.iktdev.mediaprocessing.converter.tasks

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.mediaprocessing.converter.*
import no.iktdev.mediaprocessing.converter.convert.ConvertListener
import no.iktdev.mediaprocessing.converter.convert.Converter2
import no.iktdev.mediaprocessing.shared.common.services.TaskService
import no.iktdev.mediaprocessing.shared.common.task.ConvertTaskData
import no.iktdev.mediaprocessing.shared.common.task.Task
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ConvertWorkPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Service


@EnableScheduling
@Service
class ConvertServiceV2(
    @Autowired var tasks: TaskCoordinator,
) : TaskService(), ConvertListener, TaskCoordinator.TaskEvents {
    override val log = KotlinLogging.logger {}
    override val logDir = ConverterEnv.logDirectory

    override fun getServiceId(serviceName: String): String {
        return super.getServiceId(this::class.java.simpleName)
    }


    var worker: Converter2? = null

    override fun onAttachListener() {
        tasks.addConvertTaskListener(this)
        tasks.addTaskEventListener(this)
    }


    override fun isReadyToConsume(): Boolean {
        return worker == null
    }

    override fun isTaskClaimable(task: Task): Boolean {
        return !taskManager.isTaskClaimed(referenceId = task.referenceId, eventId = task.eventId)
    }


    override fun onTaskAssigned(task: Task) {
        startConvert(task)
    }

    fun startConvert(task: Task) {
        val convert = task.data as ConvertTaskData
        worker = Converter2(convert, this)
        worker?.execute()
    }

    override fun onStarted(inputFile: String) {
        val task = assignedTask ?: return
        taskManager.markTaskAsClaimed(task.referenceId, task.eventId, serviceId)
        log.info { "Convert started for ${task.referenceId}" }

    }


    override fun onCompleted(inputFile: String, outputFiles: List<String>) {
        val task = assignedTask ?: return
        log.info { "Convert completed for ${task.referenceId}" }
        val claimSuccessful = taskManager.markTaskAsCompleted(task.referenceId, task.eventId)

        runBlocking {
            delay(1000)
            if (!claimSuccessful) {
                taskManager.markTaskAsCompleted(task.referenceId, task.eventId)
                delay(1000)
            }
            var readbackIsSuccess = taskManager.isTaskCompleted(task.referenceId, task.eventId)
            while (!readbackIsSuccess) {
                delay(1000)
                readbackIsSuccess = taskManager.isTaskCompleted(task.referenceId, task.eventId)
            }

            tasks.producer.sendMessage(
                referenceId = task.referenceId, event = KafkaEvents.EventWorkConvertPerformed,
                data = ConvertWorkPerformed(
                    status = Status.COMPLETED,
                    producedBy = serviceId,
                    derivedFromEventId = task.eventId,
                    outFiles = outputFiles
                )
            )
            onClearTask()
        }
    }

    override fun onError(inputFile: String, message: String) {
        val task = assignedTask ?: return
        super.onError(inputFile, message)
        log.info { "Convert error for ${task.referenceId}" }

        val data = ConvertWorkPerformed(
            status = Status.ERROR,
            message = message,
            producedBy = serviceId,
            derivedFromEventId = task.eventId,
            outFiles = emptyList()
        )
        tasks.producer.sendMessage(
            referenceId = task.referenceId, event = KafkaEvents.EventWorkConvertPerformed,
            data = data
        )
    }


    override fun onClearTask() {
        super.onClearTask()
        worker = null
    }

    override fun onCancelOrStopProcess(eventId: String) {
        TODO("Not yet implemented")
    }

}