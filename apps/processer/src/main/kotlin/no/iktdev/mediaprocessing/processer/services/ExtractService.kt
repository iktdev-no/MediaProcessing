package no.iktdev.mediaprocessing.processer.services

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.eventi.data.EventMetadata
import no.iktdev.eventi.data.EventStatus
import no.iktdev.mediaprocessing.processer.ProcesserEnv
import no.iktdev.mediaprocessing.processer.Reporter
import no.iktdev.mediaprocessing.processer.TaskCoordinator
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegRunner
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegTaskService
import no.iktdev.mediaprocessing.processer.ffmpeg.progress.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.processer.taskManager
import no.iktdev.mediaprocessing.shared.common.limitedWhile
import no.iktdev.mediaprocessing.shared.common.database.cal.Status
import no.iktdev.mediaprocessing.shared.common.task.Task
import no.iktdev.mediaprocessing.shared.common.contract.data.ExtractArgumentData
import no.iktdev.mediaprocessing.shared.common.contract.data.ExtractWorkPerformedEvent
import no.iktdev.mediaprocessing.shared.common.contract.data.ExtractedData
import no.iktdev.mediaprocessing.shared.common.contract.dto.ProcesserEventInfo
import no.iktdev.mediaprocessing.shared.common.contract.dto.WorkStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class ExtractService(
    @Autowired var tasks: TaskCoordinator,
    @Autowired private val reporter: Reporter
) : FfmpegTaskService(), TaskCoordinator.TaskEvents {

    fun getProducerName(): String {
        return this::class.java.simpleName
    }

    override val log = KotlinLogging.logger {}
    override val logDir = ProcesserEnv.encodeLogDirectory

    override fun getServiceId(serviceName: String): String {
        return super.getServiceId(this::class.java.simpleName)
    }

    override fun onAttachListener() {
        tasks.addExtractTaskListener(this)
        tasks.addTaskEventListener(this)
    }

    override fun isReadyToConsume(): Boolean {
        return runner?.isWorking() == false
    }

    override fun isTaskClaimable(task: Task): Boolean {
        return !taskManager.isTaskClaimed(referenceId = task.referenceId, eventId = task.eventId)
    }

    override fun onTaskAssigned(task: Task) {
        startExtract(task)
    }


    fun startExtract(event: Task) {
        val ffwrc = event.data as ExtractArgumentData
        val outFile = File(ffwrc.outputFile).also {
            it.parentFile.mkdirs()
        }
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        val setClaim = taskManager.markTaskAsClaimed(referenceId = event.referenceId, eventId = event.eventId, claimer = serviceId)
        if (setClaim) {
            log.info { "Claim successful for ${event.referenceId} extract" }
            runner = FfmpegRunner(
                inputFile = ffwrc.inputFile,
                outputFile = ffwrc.outputFile,
                arguments = ffwrc.arguments,
                logDir = logDir,
                listener = this
            )
            if (outFile.exists()) {
                if (ffwrc.arguments.firstOrNull() != "-y") {
                    this.onError(
                        ffwrc.inputFile,
                        "${this::class.java.simpleName} identified the file as already existing, either allow overwrite or delete the offending file: ${ffwrc.outputFile}"
                    )
                    // Setting consumed to prevent spamming
                    taskManager.markTaskAsCompleted(event.referenceId, event.eventId, Status.ERROR)
                    return
                }
            }
            runner?.run()
        } else {
            log.error { "Failed to set claim on referenceId: ${event.referenceId} on event ${event.task}" }
        }
    }

    override fun onStarted(inputFile: String) {
        val task = assignedTask ?: return
        taskManager.markTaskAsClaimed(task.referenceId, task.eventId, serviceId)
        sendProgress(task.referenceId, task.eventId, WorkStatus.Started)
    }

    override fun onCompleted(inputFile: String, outputFile: String) {
        val task = assignedTask ?: return
        log.info { "Extract completed for ${task.referenceId}" }
        runBlocking {
            var successfulComplete = false
            limitedWhile({!successfulComplete}, 1000 * 10, 1000) {
                taskManager.markTaskAsCompleted(task.referenceId, task.eventId)
                successfulComplete = taskManager.isTaskCompleted(task.referenceId, task.eventId)
            }

            tasks.onProduceEvent(
                ExtractWorkPerformedEvent(
                    metadata = EventMetadata(
                        referenceId = task.referenceId,
                        derivedFromEventId = task.eventId,
                        status = EventStatus.Success,
                        source = getProducerName()
                    ),
                    data = ExtractedData(
                        outputFile
                    )
                )
            )
            sendProgress(
                task.referenceId, task.eventId, status = WorkStatus.Completed, FfmpegDecodedProgress(
                    progress = 100,
                    time = "",
                    duration = "",
                    speed = "0",
                )
            )
            clearWorker()
        }
    }

    override fun onError(inputFile: String, message: String) {
        val task = assignedTask ?: return

        taskManager.markTaskAsCompleted(task.referenceId, task.eventId, Status.ERROR)

        log.error { "Extract failed for ${task.referenceId}\n$message" }
        tasks.onProduceEvent(
            ExtractWorkPerformedEvent(
                metadata = EventMetadata(
                    referenceId = task.referenceId,
                    derivedFromEventId = task.eventId,
                    status = EventStatus.Failed,
                    source = getProducerName()
                )
            )
        )
        sendProgress(
            task.referenceId, task.eventId, status = WorkStatus.Failed, progress = FfmpegDecodedProgress(
                progress = 0,
                time = "",
                duration = "",
                speed = "0",
            )
        )
        clearWorker()
    }

    override fun onProgressChanged(inputFile: String, progress: FfmpegDecodedProgress) {
        val task = assignedTask ?: return
        sendProgress(task.referenceId, task.eventId, WorkStatus.Working, progress)
    }


    fun sendProgress(referenceId: String, eventId: String, status: WorkStatus, progress: FfmpegDecodedProgress? = null) {
        val runner = runner ?: return

        val processerEventInfo = ProcesserEventInfo(
            referenceId = referenceId,
            eventId = eventId,
            status = status,
            inputFile = runner.inputFile,
            outputFiles = listOf(runner.outputFile),
            progress = progress?.toProcessProgress()
        )
        try {
            reporter.sendExtractProgress(processerEventInfo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCancelOrStopProcess(eventId: String) {
        cancelWorkIfRunning(eventId)
    }
}