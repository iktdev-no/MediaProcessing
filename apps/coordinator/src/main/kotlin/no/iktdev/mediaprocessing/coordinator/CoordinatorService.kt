package no.iktdev.mediaprocessing.coordinator

import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class CoordinatorService {

    private val progressMap = ConcurrentHashMap<String, ProgressUpdate>()

    fun updateProgress(update: ProgressUpdate) {
        progressMap[update.taskId] = update
    }

    fun getProgress(taskId: String): ProgressUpdate? =
        progressMap[taskId]
}
