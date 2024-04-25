package no.iktdev.mediaprocessing.processer.services

import kotlinx.coroutines.*
import mu.KotlinLogging
import no.iktdev.mediaprocessing.processer.*
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegWorker
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegWorkerEvents
import no.iktdev.mediaprocessing.shared.common.limitedWhile
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import no.iktdev.mediaprocessing.shared.common.getComputername
import no.iktdev.mediaprocessing.shared.contract.dto.ProcesserEventInfo
import no.iktdev.mediaprocessing.shared.contract.dto.WorkStatus
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.work.ProcesserExtractWorkPerformed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import javax.annotation.PreDestroy

@Service
class ExtractService(@Autowired override var coordinator: Coordinator, @Autowired private val reporter: Reporter): TaskCreator(coordinator) {
    private val log = KotlinLogging.logger {}
    private val logDir = ProcesserEnv.extractLogDirectory


    override val producesEvent = KafkaEvents.EventWorkExtractPerformed

    private var runner: FfmpegWorker? = null

    val serviceId = "${getComputername()}::${this.javaClass.simpleName}::${UUID.randomUUID()}"
    init {
        log.info { "Starting with id: $serviceId" }
    }

    override val requiredEvents: List<KafkaEvents>
        get() = listOf(KafkaEvents.EventWorkExtractCreated)

    override fun prerequisitesRequired(events: List<PersistentProcessDataMessage>): List<() -> Boolean> {
        return super.prerequisitesRequired(events) + listOf {
            isPrerequisiteDataPresent(events)
        }
    }

    override fun onProcessEvents(event: PersistentProcessDataMessage, events: List<PersistentProcessDataMessage>): MessageDataWrapper? {
        if (!requiredEvents.contains(event.event)) {
            return null
        }
        if (event.data !is FfmpegWorkRequestCreated) {
            return SimpleMessageData(status = Status.ERROR, message = "Invalid data (${event.data.javaClass.name}) passed for ${event.event.event}", event.eventId)
        }

        val isAlreadyClaimed = eventManager.isProcessEventClaimed(referenceId = event.referenceId, eventId = event.eventId)
        if (isAlreadyClaimed) {
            log.warn {  "Process is already claimed!" }
            return null
        }

        if (runner?.isWorking() != true) {
            startExtract(event)
        } else {
            log.warn { "Worker is already running.." }
        }
        // This should never return any other than continue or skipped
        return null
    }

    fun startExtract(event: PersistentProcessDataMessage) {
        val ffwrc = event.data as FfmpegWorkRequestCreated
        File(ffwrc.outFile).parentFile.mkdirs()
        if (!logDir.exists()) {
            logDir.mkdirs()
        }


        val setClaim = eventManager.setProcessEventClaim(referenceId = event.referenceId, eventId = event.eventId, claimer = serviceId)
        if (setClaim) {
            log.info { "Claim successful for ${event.referenceId} extract" }
            runner = FfmpegWorker(event.referenceId, event.eventId, info = ffwrc, logDir = logDir, listener = ffmpegWorkerEvents)

            if (File(ffwrc.outFile).exists() && ffwrc.arguments.firstOrNull() != "-y") {
                ffmpegWorkerEvents.onError(event.referenceId, event.eventId, ffwrc, "${this::class.java.simpleName} identified the file as already existing, either allow overwrite or delete the offending file: ${ffwrc.outFile}")
                // Setting consumed to prevent spamming
                eventManager.setProcessEventCompleted(event.referenceId, event.eventId)
                return
            }
            runner!!.run()
        } else {
            log.error { "Failed to set claim on referenceId: ${event.referenceId} on event ${event.event}" }
        }

    }

    val ffmpegWorkerEvents = object : FfmpegWorkerEvents {
        override fun onStarted(referenceId: String, eventId: String, info: FfmpegWorkRequestCreated) {
            val runner = this@ExtractService.runner
            if (runner == null || runner.referenceId.isBlank()) {
                log.error { "Can't produce start message when the referenceId is not present" }
                return
            }
            log.info { "Extract started for ${runner.referenceId}" }
            eventManager.setProcessEventClaim(runner.referenceId, runner.eventId, serviceId)
            sendProgress(referenceId, eventId, WorkStatus.Started, info)
        }

        override fun onCompleted(referenceId: String, eventId: String, info: FfmpegWorkRequestCreated) {
            val runner = this@ExtractService.runner
            if (runner == null || runner.referenceId.isBlank()) {
                log.error { "Can't produce completion message when the referenceId is not present" }
                return
            }
            log.info { "Extract completed for ${runner.referenceId}" }
            var consumedIsSuccessful = eventManager.setProcessEventCompleted(runner.referenceId, runner.eventId)
            runBlocking {

                delay(1000)
                limitedWhile({!consumedIsSuccessful}, 1000 * 10, 1000) {
                    consumedIsSuccessful = eventManager.setProcessEventCompleted(runner.referenceId, runner.eventId)
                }

                log.info { "Database is reporting extract on ${runner.referenceId} as ${if (consumedIsSuccessful) "CONSUMED" else "NOT CONSUMED"}" }
                delay(1000)



                var readbackIsSuccess = eventManager.isProcessEventCompleted(runner.referenceId, runner.eventId)
                limitedWhile({!readbackIsSuccess}, 1000 * 30, 1000) {
                    readbackIsSuccess = eventManager.isProcessEventCompleted(runner.referenceId, runner.eventId)
                    log.info { readbackIsSuccess }
                }
                log.info { "Database is reporting readback for extract on ${runner.referenceId} as ${if (readbackIsSuccess) "CONSUMED" else "NOT CONSUMED"}" }


                producer.sendMessage(referenceId = runner.referenceId, event = producesEvent,
                    ProcesserExtractWorkPerformed(
                        status = Status.COMPLETED,
                        producedBy = serviceId,
                        derivedFromEventId =  runner.eventId,
                        outFile = runner.info.outFile)
                )
                sendProgress(referenceId, eventId, WorkStatus.Completed, info)
                log.info { "Extract is releasing worker" }
                clearWorker()
            }
        }

        override fun onError(referenceId: String, eventId: String, info: FfmpegWorkRequestCreated, errorMessage: String) {
            eventManager.setProcessEventCompleted(referenceId, eventId, Status.ERROR)
            val runner = this@ExtractService.runner
            if (runner == null || runner.referenceId.isBlank()) {
                log.error { "Can't produce error message when the referenceId is not present" }
                return
            }
            log.info { "Extract failed for ${runner.referenceId}\n$errorMessage" }
            producer.sendMessage(referenceId = runner.referenceId, event = producesEvent,
                ProcesserExtractWorkPerformed(status = Status.ERROR, message = errorMessage, producedBy = serviceId, derivedFromEventId =  runner.eventId)
            )
            sendProgress(referenceId, eventId, WorkStatus.Failed, info)
            clearWorker()
        }

        override fun onProgressChanged(referenceId: String, eventId: String, info: FfmpegWorkRequestCreated, progress: FfmpegDecodedProgress) {
            sendProgress(referenceId, eventId, WorkStatus.Working, info, progress)
        }

    }

    fun sendProgress(referenceId: String, eventId: String, status: WorkStatus, info: FfmpegWorkRequestCreated, progress: FfmpegDecodedProgress? = null) {
        val processerEventInfo = ProcesserEventInfo(
            referenceId = referenceId,
            eventId = eventId,
            status = status,
            inputFile = info.inputFile,
            outputFiles = listOf(info.outFile),
            progress = progress?.toProcessProgress()
        )
        reporter.sendExtractProgress(processerEventInfo)
    }


    fun clearWorker() {
        this.runner = null
        coordinator.readNextAvailableMessageWithEvent(KafkaEvents.EventWorkExtractCreated)
    }

    @PreDestroy
    fun shutdown() {
        runner?.cancel("Stopping application")
    }
}