package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreContentAndMetadataTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MigrateToContentStoreTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.StoreContentAndMetadataTask
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Component
class StoreContentAndMetadataTaskListener: TaskListener(TaskType.MIXED) {
    @Autowired
    lateinit var streamitRestTemplate: RestTemplate

    override fun getWorkerId(): String {
        return "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"
    }

    override fun supports(task: Task): Boolean {
        return task is StoreContentAndMetadataTask
    }

    override suspend fun onTask(task: Task): Event? {
        val pickedTask = task as? StoreContentAndMetadataTask ?: return null

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val entity = HttpEntity(pickedTask.data, headers)

        val response = try {
            val res = streamitRestTemplate.exchange(
                "open/api/mediaprocesser/import",
                HttpMethod.POST,
                entity,
                Void::class.java,
            )
            res.statusCode.is2xxSuccessful
        } catch (e: Exception) {
            false
        }

        return StoreContentAndMetadataTaskResultEvent(
            if (response) TaskStatus.Completed else TaskStatus.Failed
        )
    }

}