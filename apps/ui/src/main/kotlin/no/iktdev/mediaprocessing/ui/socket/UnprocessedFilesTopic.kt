package no.iktdev.mediaprocessing.ui.socket

import no.iktdev.eventi.database.withTransaction
import no.iktdev.exfl.observable.ObservableList
import no.iktdev.mediaprocessing.shared.common.contract.data.MediaProcessStartEvent
import no.iktdev.mediaprocessing.shared.common.contract.data.az
import no.iktdev.mediaprocessing.shared.common.contract.jsonToEvent
import no.iktdev.mediaprocessing.shared.common.database.tables.events
import no.iktdev.mediaprocessing.shared.common.database.tables.files
import no.iktdev.mediaprocessing.shared.common.database.tables.filesProcessed
import no.iktdev.mediaprocessing.ui.WebSocketMonitoringService
import no.iktdev.mediaprocessing.ui.eventDatabase
import no.iktdev.mediaprocessing.ui.socket.UnprocessedFilesTopic.DatabaseData.filesInProcess
import no.iktdev.mediaprocessing.ui.socket.UnprocessedFilesTopic.DatabaseData.pullUnprocessedFiles
import no.iktdev.mediaprocessing.ui.socket.UnprocessedFilesTopic.DatabaseData.unprocessedFiles
import no.iktdev.mediaprocessing.ui.socket.UnprocessedFilesTopic.DatabaseData.update
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Controller
import org.springframework.web.client.RestTemplate
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Controller
@EnableScheduling
class UnprocessedFilesTopic(
    @Autowired private val template: SimpMessagingTemplate?,
    @Autowired private val coordinatorTemplate: RestTemplate,
    @Autowired private val webSocketMonitoringService: WebSocketMonitoringService
) {


    object DatabaseData {
        private const val PULL_MIN_INTERVAL = 10_000 // 10 sekunder
        private var lastPoll: Long = 0
        var unprocessedFiles: List<FileInfo> = emptyList()
            private set
        var filesInProcess: List<FileInfo> = emptyList()
            private set

        // Funksjon som oppdaterer dataen hvis det har gått mer enn PULL_MIN_INTERVAL siden forrige oppdatering
        fun update() {
            val currentTime = System.currentTimeMillis()

            // Sjekk om det har gått mer enn 10 sekunder (10000 ms) siden siste oppdatering
            if (currentTime - lastPoll >= PULL_MIN_INTERVAL) {
                // Oppdater tidspunktet for siste poll
                lastPoll = currentTime

                // Oppdater dataene ved å hente nye verdier
                val filesNotCompleted = pullUnprocessedFiles()
                filesInProcess = pullUncompletedFiles()
                unprocessedFiles = filesNotCompleted.filter { u -> !filesInProcess.any { p -> p.checksum == u.checksum } }


            }
        }

        private fun pullUnprocessedFiles(): List<FileInfo> = withTransaction(eventDatabase.database) {
            val found = files.select {
                files.checksum notInSubQuery filesProcessed.slice(filesProcessed.checksum).selectAll()
            }.mapNotNull {
                FileInfo(
                    it[files.baseName],
                    it[files.fileName],
                    it[files.checksum]
                )
            }
            unprocessedFiles = found
            found//.filter { File(it.fileName).exists() }
        } ?: emptyList()

        private fun pullUncompletedFiles(): List<FileInfo> = withTransaction(eventDatabase.database) {
            val eventStartedFiles = events.select {
                events.event eq "ProcessStarted"
            }.mapNotNull { it[events.data].jsonToEvent(it[events.event]) }
                .mapNotNull { it.az<MediaProcessStartEvent>() }
                .mapNotNull { it.data?.file }
            unprocessedFiles.filter { it.fileName in eventStartedFiles }.also {
                filesInProcess = it
            }
        } ?: emptyList()
    }

    data class UnprocessedFiles(
        val available: List<FileInfo>,
        val inProcess: List<FileInfo>
    )

    @MessageMapping("/files/unprocessed")
    fun postUnProcessedFiles() {
        update()
        template?.convertAndSend("/topic/files/unprocessed", UnprocessedFiles(
            available = unprocessedFiles,
            inProcess = filesInProcess
        ))
    }
}

data class FileInfo(
    val name: String,
    val fileName: String,
    val checksum: String
)