package no.iktdev.mediaprocessing.shared.common.event_task_contract

import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MediaReadTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MetadataSearchTask
import no.iktdev.eventi.models.Task

object TaskRegistry {
    fun getTasks(): List<Class<out Task>> {
        return listOf(
            ConvertTask::class.java,
            EncodeTask::class.java,
            ExtractSubtitleTask::class.java,
            MediaReadTask::class.java,
            MetadataSearchTask::class.java
        )
    }
}