package no.iktdev.mediaprocessing.coordinator.services

import mu.KotlinLogging
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.dto.requests.StartProcessRequest
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartFlow
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.stereotype.Service
import java.util.*

@Service
class CommandService {
    val log = KotlinLogging.logger {}

    fun startProcess(request: StartProcessRequest): StartResult {
        return try {
            val file = IFile(request.fileUri)
            if (file.notExist()) {
                throw IllegalArgumentException("File does not exists at ${request.fileUri}")
            }
            if (!file.canRead()) {
                throw IllegalStateException("File is not readable ${request.fileUri}")
            }

            val startProcessingEvent = StartProcessingEvent(
                data = StartData(
                    fileUri = request.fileUri,
                    operation = request.operationTypes,
                    flow = StartFlow.Manual
                )
            ).newReferenceId()
            EventStore.persist(startProcessingEvent)
            StartResult.Accepted(startProcessingEvent.referenceId)
        } catch (e: Exception) {
            log.error { e }
            StartResult.Rejected("Failed to start process for file ${request.fileUri}, with the following reason: ${e.message}")
        }
    }


    sealed class StartResult {
        data class Accepted(val referenceId: UUID) : StartResult()
        data class Rejected(val reason: String) : StartResult()
    }

}