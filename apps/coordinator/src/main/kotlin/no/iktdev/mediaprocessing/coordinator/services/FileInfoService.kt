package no.iktdev.mediaprocessing.coordinator.services

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.InputFileInfo
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2

@Service
class FileInfoService(
    private val coordinatorEnv: CoordinatorEnv
) {

    private val lock = Any()
    private val gson = Gson()



    protected fun getFilesInEventsWithReference(): Map<String, List<UUID>> {
        val startedEvents = EventStore.getStartEvents()
            .map { it.toEvent() }.filterIsInstance<StartProcessingEvent>()
        val grouped = startedEvents.groupBy { it.data.fileUri }.map { (fileUri, events) ->
            fileUri to events.map { it.referenceId }
        }.toMap()
        return grouped
    }

    data class PreservedFile(
        val fileUri: String,
        val fileName: String,
    )

    fun getPreservedInputFiles(): List<PreservedFile> {
        synchronized(lock) {
            val preserveFile = coordinatorEnv.preserveFile
            if (!preserveFile.exists()) {
                return emptyList()
            }

            val items = fromJsonList<PreservedFile>(preserveFile.readText())
            return items
        }
    }


    fun getFilesWithPreservedInputFiles(): List<InputFileInfo> {
        val preservedFiles = getPreservedInputFiles()
        val registry = getFilesInEventsWithReference()
            .map { (fileUri, referenceIds) ->
                InputFileInfo(
                    usedInReferences = referenceIds.map { it.toString() },
                    fileUri = fileUri,
                    fileName = IFile(fileUri).name,
                    preserved = preservedFiles.any { it.fileUri == fileUri }
                )
            }
        return registry
    }

    fun setFilesAsPreserved(files: List<String>): List<InputFileInfo>  {
        val preservedFiles = getPreservedInputFiles().toMutableList()
        val newFiles = files.map { file -> IFile(file) }
            .map { PreservedFile(it.path, it.name) }
        preservedFiles.removeAll { existing -> newFiles.any { it.fileUri == existing.fileUri } }
        preservedFiles.addAll(newFiles)
        synchronized(lock) {
            val json = gson.toJson(preservedFiles)
            coordinatorEnv.preserveFile.writeText(json)
        }
        return getFilesWithPreservedInputFiles()
    }


    private inline fun <reified T> fromJsonList(json: String): List<T> {
        val type = object : TypeToken<List<T>>() {}.type
        return gson.fromJson(json, type)
    }

}