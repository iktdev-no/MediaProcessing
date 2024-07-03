package no.iktdev.mediaprocessing.processer.services

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.mediaprocessing.processer.ProcesserEnv
import no.iktdev.mediaprocessing.processer.Reporter
import no.iktdev.mediaprocessing.processer.TaskCoordinator
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegRunner
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegTaskService
import no.iktdev.mediaprocessing.processer.ffmpeg.progress.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.processer.taskManager
import no.iktdev.mediaprocessing.shared.common.ClaimableTask
import no.iktdev.mediaprocessing.shared.common.task.FfmpegTaskData
import no.iktdev.mediaprocessing.shared.common.task.Task
import no.iktdev.mediaprocessing.shared.contract.dto.ProcesserEventInfo
import no.iktdev.mediaprocessing.shared.contract.dto.WorkStatus
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.work.ProcesserEncodeWorkPerformed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.time.Duration

@Service
class EncodeServiceV2(
    @Autowired var tasks: TaskCoordinator,
    @Autowired private val reporter: Reporter
) : FfmpegTaskService(), TaskCoordinator.TaskEvents {

    override val log = KotlinLogging.logger {}
    override val logDir = ProcesserEnv.encodeLogDirectory

    override fun getServiceId(serviceName: String): String {
        return super.getServiceId(this::class.java.simpleName)
    }

    override fun onAttachListener() {
        tasks.addEncodeTaskListener(this)
        tasks.addTaskEventListener(this)
    }

    override fun isReadyToConsume(): Boolean {
        return runner?.isWorking() == false
    }

    override fun isTaskClaimable(task: Task): Boolean {
        return !taskManager.isTaskClaimed(referenceId = task.referenceId, eventId = task.eventId)
    }

    override fun onTaskAssigned(task: Task) {
        startEncode(task)
    }


    fun startEncode(event: Task) {
        val ffwrc = event.data as FfmpegTaskData
        val outFile = File(ffwrc.outFile)
        outFile.parentFile.mkdirs()
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        val setClaim = taskManager.markTaskAsClaimed(referenceId = event.referenceId, eventId = event.eventId, claimer = serviceId)

        if (setClaim) {
            log.info { "Claim successful for ${event.referenceId} encode" }
            runner = FfmpegRunner(
                inputFile = ffwrc.inputFile,
                outputFile = ffwrc.outFile,
                arguments = ffwrc.arguments,
                logDir = logDir, listener = this
            )
            if (outFile.exists()) {
                if (ffwrc.arguments.firstOrNull() != "-y") {
                    this.onError(
                        ffwrc.inputFile,
                        "${this::class.java.simpleName} identified the file as already existing, either allow overwrite or delete the offending file: ${ffwrc.outFile}"
                    )
                    // Setting consumed to prevent spamming
                    taskManager.markTaskAsCompleted(event.referenceId, event.eventId, Status.ERROR)
                    return
                }
            }
            runner?.run(true)

        } else {
            log.error { "Failed to set claim on referenceId: ${event.referenceId} on event ${event.task}" }
        }
    }

    override fun onStarted(inputFile: String) {
        val task = assignedTask ?: return
        taskManager.markTaskAsClaimed(task.referenceId, task.eventId, serviceId)
        sendProgress(
            task.referenceId, task.eventId, status = WorkStatus.Started, FfmpegDecodedProgress(
                progress = 0,
                time = "Unkown",
                duration = "Unknown",
                speed = "0",
            )
        )
        log.info { "Encode started for ${task.referenceId}" }

        runner?.scope?.launch {
            log.info { "Encode keep-alive started for ${task.referenceId}" }
            while (runner?.isAlive() == true) {
                delay(Duration.ofMinutes(5).toMillis())
                taskManager.refreshTaskClaim(task.referenceId, task.eventId, serviceId)
            }
        }
    }

    override fun onCompleted(inputFile: String, outputFile: String) {
        val task = assignedTask ?: return
        log.info { "Encode completed for ${task.referenceId}" }
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
                referenceId = task.referenceId, event = KafkaEvents.EventWorkEncodePerformed,
                data = ProcesserEncodeWorkPerformed(
                    status = Status.COMPLETED,
                    producedBy = serviceId,
                    derivedFromEventId = task.derivedFromEventId,
                    outFile = outputFile
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

        log.info { "Encode failed for ${task.referenceId}\n$message" }
        tasks.producer.sendMessage(
            referenceId = task.referenceId, event = KafkaEvents.EventWorkEncodePerformed,
            data = ProcesserEncodeWorkPerformed(
                status = Status.ERROR,
                message = message,
                producedBy = serviceId,
                derivedFromEventId = task.derivedFromEventId,
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



    fun sendProgress(
        referenceId: String,
        eventId: String,
        status: WorkStatus,
        progress: FfmpegDecodedProgress? = null
    ) {
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
            reporter.sendEncodeProgress(processerEventInfo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCancelOrStopProcess(eventId: String) {
        cancelWorkIfRunning(eventId)
    }
}