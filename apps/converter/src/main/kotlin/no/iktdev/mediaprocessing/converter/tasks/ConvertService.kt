package no.iktdev.mediaprocessing.converter.tasks

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.eventi.data.EventMetadata
import no.iktdev.eventi.data.EventStatus
import no.iktdev.mediaprocessing.converter.*
import no.iktdev.mediaprocessing.converter.convert.ConvertListener
import no.iktdev.mediaprocessing.converter.convert.Converter2
import no.iktdev.mediaprocessing.shared.common.persistance.Status
import no.iktdev.mediaprocessing.shared.common.services.TaskService
import no.iktdev.mediaprocessing.shared.common.task.Task
import no.iktdev.mediaprocessing.shared.contract.data.ConvertData
import no.iktdev.mediaprocessing.shared.contract.data.ConvertWorkPerformed
import no.iktdev.mediaprocessing.shared.contract.data.ConvertedData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class ConvertService(
    @Autowired var tasks: TaskCoordinator,
) : TaskService(), ConvertListener, TaskCoordinator.TaskEvents {

    fun getProducerName(): String {
        return this::class.java.simpleName
    }

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
        val convert = task.data as ConvertData
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

            tasks.onProduceEvent(ConvertWorkPerformed(
                metadata = EventMetadata(
                    referenceId = task.referenceId,
                    derivedFromEventId = task.eventId,
                    status = EventStatus.Success,
                    source = getProducerName()
                ),
                data = ConvertedData(
                    outputFiles = outputFiles
                )
            ))
            onClearTask()
        }
    }

    override fun onError(inputFile: String, message: String) {
        val task = assignedTask ?: return
        super.onError(inputFile, message)
        log.info { "Convert error for ${task.referenceId}\nmessage: $message" }

        taskManager.markTaskAsCompleted(task.referenceId, task.eventId, Status.ERROR)

        tasks.onProduceEvent(ConvertWorkPerformed(
            metadata = EventMetadata(
                referenceId = task.referenceId,
                derivedFromEventId = task.eventId,
                status = EventStatus.Failed,
                source = getProducerName()
            )
        ))
        onClearTask()
    }


    override fun onClearTask() {
        super.onClearTask()
        worker = null
    }

    override fun onCancelOrStopProcess(eventId: String) {
        TODO("Not yet implemented")
    }

}