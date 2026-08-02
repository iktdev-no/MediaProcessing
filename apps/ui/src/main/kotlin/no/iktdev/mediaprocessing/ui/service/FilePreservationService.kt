package no.iktdev.mediaprocessing.ui.service

import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.common.dto.files.PreservedFile
import no.iktdev.mediaprocessing.shared.common.files.FilePreservationImpl
import no.iktdev.mediaprocessing.shared.database.stores.FilePreservationStore
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2

@Service
class FilePreservationService(
): FilePreservationImpl(FilePreservationStore) {

    protected fun getFilesInEventsWithReference(): Map<IFile, List<UUID>> {
        val startedEvents = EventStore.getStartEvents()
            .map { it.toEvent() }.filterIsInstance<StartProcessingEvent>()
        val grouped = startedEvents.groupBy { it.data.fileUri }.map { (fileUri, events) ->
            IFile(fileUri) to events.map { it.referenceId }
        }.toMap()
        return grouped
    }

    fun getPreservedInputFiles(): List<PreservedFile> {
        return getPreservedFiles()
    }


    fun getFilesWithPreservedInputFiles(): List<PreservedFile> {
        val preservedFiles = getAllFiles() // Antar dette returnerer List<PreservedFile> fra databasen
        val registry = getFilesInEventsWithReference()

        // 1. Lag en map med fileName som nøkkel, og start med de fra databasen (disse har høyest prioritet)
        val mergedFiles = preservedFiles.associateBy { it.fileName }.toMutableMap()

        // 2. Legg til fra registry KUN hvis filnavnet ikke finnes fra før
        for ((ifile, referenceIds) in registry) {
            val fileName = ifile.name

            // putIfAbsent sørger for at vi IKKE overskriver den som allerede ligger der fra databasen
            mergedFiles.putIfAbsent(
                fileName,
                PreservedFile(
                    fileName = fileName,
                    filePath = ifile.absolutePath,
                    preserved = false,
                    usedInReferences = referenceIds
                )
            )
        }

        // 3. Returner verdiene fra map-en som en liste
        return mergedFiles.values.toList()
    }

    fun setFilesAsPreserved(files: List<String>): List<PreservedFile>  {
        val ifs = files.map { file -> IFile(file) }
        preserveFiles(ifs)
        return getPreservedFiles()
    }

}