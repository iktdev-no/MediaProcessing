package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress.Progress
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class ProgressManagerService(
    private val progressTranslatorService: ProgressTranslatorService
) {
    private val progressMap = ConcurrentHashMap<String, Progress>()


    fun getProgress(taskId: String): Progress? =
        progressMap[taskId]

    fun getProgress(): List<Progress> =
        progressMap.values.toList()

    fun updateCache(progress: Progress) {
        progressMap[progress.taskId] = progress
    }

    fun onReceivedProgressUpdate(update: ProgressUpdate): Progress {
        val progress = progressTranslatorService.translate(update)
        updateCache(progress)
        return progress

    }
}