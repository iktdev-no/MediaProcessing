package no.iktdev.mediaprocessing.coordinator.services

import mu.KotlinLogging
import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.coordinator.eventDatabase
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.database.tables.files
import no.iktdev.mediaprocessing.shared.common.extended.isSupportedVideoFile
import no.iktdev.mediaprocessing.shared.common.md5
import org.jetbrains.exposed.sql.insertIgnore
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class UnattendedIndexing {
    private val logger = KotlinLogging.logger {}

    @Scheduled(fixedDelay = 60_000*60)
    fun indexContent() {
        val allFiles = SharedConfig.incomingContent.flatMap { folder ->
            logger.info { "Performing indexing of folder: ${folder.name}" }
            folder.walkTopDown()
                .filter { it.isFile && it.isSupportedVideoFile() }
                .toMutableList()
        }
        val ignoredParents = allFiles
            .asSequence()
            .mapNotNull { it.parentFile }
            .filter { parent -> parent.resolve(".ignore").exists() }
            .toSet()

        val fileList = allFiles
            .filter { file -> file.parentFile !in ignoredParents }

        fileList.forEach { file ->
            withTransaction(eventDatabase.database) {
                files.insertIgnore {
                    it[this.fileName] = file.absolutePath
                    it[this.baseName] = file.nameWithoutExtension
                    it[this.folder] = file.parentFile.absolutePath
                    it[this.checksum] = file.md5()
                }
            }
        }
        logger.info { "Indexing completed" }
        /*val storedFiles = withTransaction(eventDatabase.database) {
            files.selectAll()
                .mapNotNull { it[files.fileName] }
        }?.forEach { file ->
            if (!File(file).exists()) {
                logger.info { "Detected file no longer existing. Performing removal i db" }
                files.deleteWhere {
                    (fileName eq file)
                }
            }
        }*/
    }
}