package no.iktdev.mediaprocessing.coordinator.services

import mu.KotlinLogging
import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.coordinator.eventDatabase
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.database.tables.files
import no.iktdev.mediaprocessing.shared.common.extended.isSupportedVideoFile
import no.iktdev.mediaprocessing.shared.common.md5
import no.iktdev.streamit.library.db.withTransaction
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File

@Service
@EnableScheduling
class UnattendedIndexing {
    private val logger = KotlinLogging.logger {}

    @Scheduled(fixedDelay = 60_000*60)
    fun indexContent() {
        logger.info { "Performing indexing of input root: ${SharedConfig.inputRoot.absolutePath}" }
        val foundFiles: MutableList<File> = mutableListOf()
        SharedConfig.inputRoot.walkTopDown().filter { it.isFile && it.isSupportedVideoFile() }.also {
            foundFiles.addAll(it)
        }

        withTransaction(eventDatabase.database) {
            foundFiles.forEach { file ->
                files.insertIgnore {
                    it[this.fileName] = file.absolutePath
                    it[this.baseName] = file.nameWithoutExtension
                    it[this.folder] = file.parentFile.absolutePath
                    it[this.checksum] = file.md5()
                }
            }
        }
        logger.info { "Indexing completed" }

    }
}