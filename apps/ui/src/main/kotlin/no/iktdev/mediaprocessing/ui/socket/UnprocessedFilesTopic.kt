package no.iktdev.mediaprocessing.ui.socket

import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.shared.common.contract.dto.EventRequest
import no.iktdev.mediaprocessing.shared.common.database.tables.files
import no.iktdev.mediaprocessing.shared.common.database.tables.filesProcessed
import no.iktdev.mediaprocessing.ui.UIEnv
import no.iktdev.mediaprocessing.ui.eventsDatabase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Controller
import org.springframework.web.client.RestTemplate
import java.io.File

@Controller
@EnableScheduling
class UnprocessedFilesTopic(
    @Autowired private val template: SimpMessagingTemplate?,
    @Autowired private val coordinatorTemplate: RestTemplate,
) {
    fun pullUnprocessedFiles(): List<FileInfo> = withTransaction(eventsDatabase.database) {
        files.select {
            files.checksum notInSubQuery filesProcessed.slice(filesProcessed.checksum).selectAll()
        }.mapNotNull {
            FileInfo(
                it[files.baseName],
                it[files.fileName],
                it[files.checksum]
            )
        }.filter { File(it.fileName).exists() }
    } ?: emptyList()


    @MessageMapping("/files")
    fun getUnprocessedFiles() {
        refreshUnprocessedFiles()
    }
    @MessageMapping("/request/process")
    fun requestProcess(@Payload data: EventRequest) {
        val req = coordinatorTemplate.postForEntity("/request/all", data, String::class.java)
        log.info { "RequestProcess report:\n\tStatus: ${req.statusCode}\n\tMessage: ${req.body}" }
    }



    @Scheduled(fixedDelay = 5_000)
    fun refreshUnprocessedFiles() {
        val unprocessedFiles = pullUnprocessedFiles()
        template?.convertAndSend("/topic/files/unprocessed", unprocessedFiles)
    }
}

data class FileInfo(
    val name: String,
    val fileName: String,
    val checksum: String
)