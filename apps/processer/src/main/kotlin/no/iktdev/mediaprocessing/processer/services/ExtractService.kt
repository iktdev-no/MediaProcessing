package no.iktdev.mediaprocessing.processer.services

import kotlinx.coroutines.*
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.processer.Coordinator
import no.iktdev.mediaprocessing.processer.TaskCreator
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegWorker
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegWorkerEvents
import no.iktdev.mediaprocessing.shared.common.limitedWhile
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import no.iktdev.mediaprocessing.processer.ProcesserEnv
import no.iktdev.mediaprocessing.shared.common.getComputername
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.work.ProcesserExtractWorkPerformed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import javax.annotation.PreDestroy

@Service
class ExtractService(@Autowired override var coordinator: Coordinator): TaskCreator(coordinator) {
    private val log = KotlinLogging.logger {}
    private val logDir = ProcesserEnv.extractLogDirectory


    override val producesEvent = KafkaEvents.EVENT_WORK_EXTRACT_PERFORMED

    val scope = Coroutines.io()

    private var runner: FfmpegWorker? = null
    private var runnerJob: Job? = null

    val serviceId = "${getComputername()}::${this.javaClass.simpleName}::${UUID.randomUUID()}"
    init {
        log.info { "Starting with id: $serviceId" }
    }

    override val requiredEvents: List<KafkaEvents>
        get() = listOf(KafkaEvents.EVENT_WORK_EXTRACT_CREATED)

    override fun prerequisitesRequired(events: List<PersistentProcessDataMessage>): List<() -> Boolean> {
        return super.prerequisitesRequired(events) + listOf {
            isPrerequisiteDataPresent(events)
        }
    }

    override fun onProcessEvents(event: PersistentProcessDataMessage, events: List<PersistentProcessDataMessage>): MessageDataWrapper? {
        if (requiredEvents.contains(event.event)) {
            return null
        }
        if (event.data !is FfmpegWorkRequestCreated) {
            return SimpleMessageData(status = Status.ERROR, message = "Invalid data (${event.data.javaClass.name}) passed for ${event.event.event}")
        }

        val isAlreadyClaimed = PersistentDataReader().isProcessEventAlreadyClaimed(referenceId = event.referenceId, eventId = event.eventId)
        if (isAlreadyClaimed) {
            log.warn {  "Process is already claimed!" }
            return null
        }

        if (runnerJob?.isActive != true) {
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


        val setClaim = PersistentDataStore().setProcessEventClaim(referenceId = event.referenceId, eventId = event.eventId, claimedBy = serviceId)
        if (setClaim) {
            log.info { "Claim successful for ${event.referenceId} extract" }
            runner = FfmpegWorker(event.referenceId, event.eventId, info = ffwrc, logDir = logDir, listener = ffmpegWorkerEvents)

            if (File(ffwrc.outFile).exists() && ffwrc.arguments.firstOrNull() != "-y") {
                ffmpegWorkerEvents.onError(ffwrc, "${this::class.java.simpleName} identified the file as already existing, either allow overwrite or delete the offending file: ${ffwrc.outFile}")
                return
            }
            runnerJob = scope.launch {
                runner!!.run()
            }
        } else {
            log.error { "Failed to set claim on referenceId: ${event.referenceId} on event ${event.event}" }
        }

    }

    val ffmpegWorkerEvents = object : FfmpegWorkerEvents {
        override fun onStarted(info: FfmpegWorkRequestCreated) {
            val runner = this@ExtractService.runner
            if (runner == null || runner.referenceId.isBlank()) {
                log.error { "Can't produce start message when the referenceId is not present" }
                return
            }
            log.info { "Extract started for ${runner.referenceId}" }
            PersistentDataStore().setProcessEventClaim(runner.referenceId, runner.eventId, serviceId)
            sendState(info, false)
        }

        override fun onCompleted(info: FfmpegWorkRequestCreated) {
            val runner = this@ExtractService.runner
            if (runner == null || runner.referenceId.isBlank()) {
                log.error { "Can't produce completion message when the referenceId is not present" }
                return
            }
            log.info { "Extract completed for ${runner.referenceId}" }
            var consumedIsSuccessful = PersistentDataStore().setProcessEventCompleted(runner.referenceId, runner.eventId, serviceId)
            runBlocking {

                delay(1000)
                limitedWhile({!consumedIsSuccessful}, 1000 * 10, 1000) {
                    consumedIsSuccessful = PersistentDataStore().setProcessEventCompleted(runner.referenceId, runner.eventId, serviceId)
                }

                log.info { "Database is reporting extract on ${runner.referenceId} as ${if (consumedIsSuccessful) "CONSUMED" else "NOT CONSUMED"}" }
                delay(1000)



                var readbackIsSuccess = PersistentDataReader().isProcessEventDefinedAsConsumed(runner.referenceId, runner.eventId, serviceId)
                limitedWhile({!readbackIsSuccess}, 1000 * 30, 1000) {
                    readbackIsSuccess = PersistentDataReader().isProcessEventDefinedAsConsumed(runner.referenceId, runner.eventId, serviceId)
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
                log.info { "Extract is releasing worker" }
                clearWorker()
            }
        }

        override fun onError(info: FfmpegWorkRequestCreated, errorMessage: String) {
            val runner = this@ExtractService.runner
            if (runner == null || runner.referenceId.isBlank()) {
                log.error { "Can't produce error message when the referenceId is not present" }
                return
            }
            log.info { "Extract failed for ${runner.referenceId}" }
            producer.sendMessage(referenceId = runner.referenceId, event = producesEvent,
                ProcesserExtractWorkPerformed(status = Status.ERROR, message = errorMessage, producedBy = serviceId, derivedFromEventId =  runner.eventId)
            )
            sendState(info, ended= true)
            clearWorker()
        }

        override fun onProgressChanged(info: FfmpegWorkRequestCreated, progress: FfmpegDecodedProgress) {
            // None as this will not be running with progress
        }

    }

    fun sendState(info: FfmpegWorkRequestCreated, ended: Boolean) {

    }


    fun clearWorker() {
        this.runner?.scope?.cancel()
        this.runner = null
    }

    @PreDestroy
    fun shutdown() {
        scope.cancel()
        runner?.scope?.cancel("Stopping application")
    }
}