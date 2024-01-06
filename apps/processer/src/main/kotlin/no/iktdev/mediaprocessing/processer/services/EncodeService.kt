package no.iktdev.mediaprocessing.processer.services

import kotlinx.coroutines.*
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.processer.Tasks
import no.iktdev.mediaprocessing.processer.TaskCreator
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegWorker
import no.iktdev.mediaprocessing.processer.ffmpeg.FfmpegWorkerEvents
import no.iktdev.mediaprocessing.processer.getComputername
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.FfmpegWorkRequestCreated
import no.iktdev.mediaprocessing.processer.ProcesserEnv
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import javax.annotation.PreDestroy

@Service
class EncodeService: TaskCreator() {
    private val log = KotlinLogging.logger {}
    private val logDir = ProcesserEnv.encodeLogDirectory

    val producesEvent = KafkaEvents.EVENT_WORK_ENCODE_PERFORMED

    val scope = Coroutines.io()
    private var runner: FfmpegWorker? = null
    private var runnerJob: Job? = null
    val encodeServiceId = "${getComputername()}::${this.javaClass.simpleName}::${UUID.randomUUID()}"
    init {
        log.info { "Starting encode service with id: $encodeServiceId" }
    }

    override val requiredEvents: List<KafkaEvents>
        get() = listOf(KafkaEvents.EVENT_WORK_ENCODE_CREATED)

    override fun getListener(): Tasks {
        return Tasks(producesEvent, this)
    }


    override fun prerequisitesRequired(events: List<PersistentProcessDataMessage>): List<() -> Boolean> {
        return super.prerequisitesRequired(events) + listOf {
            isPrerequisiteDataPresent(events)
        }
    }

    override fun onProcessEvents(event: PersistentProcessDataMessage, events: List<PersistentProcessDataMessage>): MessageDataWrapper? {
        if (event.data !is FfmpegWorkRequestCreated) {
            return MessageDataWrapper(status = Status.ERROR, message = "Invalid data (${event.data.javaClass.name}) passed for ${event.event.event}")
        }

        val isAlreadyClaimed = PersistentDataReader().isProcessEventAlreadyClaimed(referenceId = event.referenceId, eventId = event.eventId)
        if (isAlreadyClaimed) {
            log.warn {  "Process is already claimed!" }
            return null
        }

        if (runnerJob?.isActive != true) {
            startEncode(event)
        } else {
            log.warn { "Worker is already running.." }
        }
        // This should never return any other than continue or skipped
        return null
    }

    fun startEncode(event: PersistentProcessDataMessage) {
        val ffwrc = event.data as FfmpegWorkRequestCreated
        File(ffwrc.outFile).parentFile.mkdirs()
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        val setClaim = PersistentDataStore().setProcessEventClaim(referenceId = event.referenceId, eventId = event.eventId, claimedBy = encodeServiceId)
        if (setClaim) {
            log.info { "Claim successful for ${event.referenceId} encode" }
            runner = FfmpegWorker(event.referenceId, event.eventId, info = ffwrc, logDir = logDir, listener = ffmpegWorkerEvents )
            if (File(ffwrc.outFile).exists() && ffwrc.arguments.firstOrNull() != "-y") {
                ffmpegWorkerEvents.onError(ffwrc, "${this::class.java.simpleName} identified the file as already existing, either allow overwrite or delete the offending file: ${ffwrc.outFile}")
                return
            }
            runnerJob = scope.launch {
                runner!!.runWithProgress()
            }

        } else {
            log.error { "Failed to set claim on referenceId: ${event.referenceId} on event ${event.event}" }
        }
    }

    val ffmpegWorkerEvents = object : FfmpegWorkerEvents {
        override fun onStarted(info: FfmpegWorkRequestCreated) {
            val runner = this@EncodeService.runner
            if (runner == null || runner.referenceId.isBlank()) {
                log.error { "Can't produce start message when the referenceId is not present" }
                return
            }
            log.info { "Encode started for ${runner.referenceId}" }
            PersistentDataStore().setProcessEventClaim(runner.referenceId, runner.eventId, encodeServiceId)
            sendProgress(info, null, false)

            scope.launch {
                while (runnerJob?.isActive == true) {
                    delay(java.time.Duration.ofMinutes(5).toMillis())
                    PersistentDataStore().updateCurrentProcessEventClaim(runner.referenceId, runner.eventId, encodeServiceId)
                }
            }
        }

        override fun onCompleted(info: FfmpegWorkRequestCreated) {
            val runner = this@EncodeService.runner
            if (runner == null || runner.referenceId.isBlank()) {
                log.error { "Can't produce completion message when the referenceId is not present" }
                return
            }
            log.info { "Encode completed for ${runner.referenceId}" }
            val consumedIsSuccessful = PersistentDataStore().setProcessEventCompleted(runner.referenceId, runner.eventId, encodeServiceId)
            runBlocking {
                delay(1000)
                if (!consumedIsSuccessful) {
                    PersistentDataStore().setProcessEventCompleted(runner.referenceId, runner.eventId, encodeServiceId)
                }
                delay(1000)
                var readbackIsSuccess = PersistentDataReader().isProcessEventDefinedAsConsumed(runner.referenceId, runner.eventId, encodeServiceId)

                while (!readbackIsSuccess) {
                    delay(1000)
                    readbackIsSuccess = PersistentDataReader().isProcessEventDefinedAsConsumed(runner.referenceId, runner.eventId, encodeServiceId)
                }
                producer.sendMessage(referenceId = runner.referenceId, event = producesEvent,
                    FfmpegWorkPerformed(status = Status.COMPLETED, producedBy = encodeServiceId, derivedFromEventId =  runner.eventId))
                clearWorker()
            }

        }

        override fun onError(info: FfmpegWorkRequestCreated, errorMessage: String) {
            val runner = this@EncodeService.runner
            if (runner == null || runner.referenceId.isBlank()) {
                log.error { "Can't produce error message when the referenceId is not present" }
                return
            }
            log.info { "Encode failed for ${runner.referenceId}" }
            producer.sendMessage(referenceId = runner.referenceId, event = producesEvent,
                FfmpegWorkPerformed(status = Status.ERROR, message = errorMessage, producedBy = encodeServiceId, derivedFromEventId =  runner.eventId))
            sendProgress(info = info, ended = true)
            clearWorker()
        }

        override fun onProgressChanged(info: FfmpegWorkRequestCreated, progress: FfmpegDecodedProgress) {
            sendProgress(info, progress, false)
        }

    }

    fun sendProgress(info: FfmpegWorkRequestCreated, progress: FfmpegDecodedProgress? = null, ended: Boolean) {
        // TODO: Implementation
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