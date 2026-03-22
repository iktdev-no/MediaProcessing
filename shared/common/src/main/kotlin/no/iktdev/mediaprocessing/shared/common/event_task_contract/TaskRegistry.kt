package no.iktdev.mediaprocessing.shared.common.event_task_contract

import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.LinearEncodeTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MediaReadTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MetadataSearchTask
import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.CoverDownloadTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.FilePrepareForWorkTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MigrateToContentStoreTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.SegmentedEncodeTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.StoreContentAndMetadataTask

object TaskRegistry {
    fun getTasks(): List<Class<out Task>> {
        return listOf(
            ConvertTask::class.java,
            CoverDownloadTask::class.java,

            LinearEncodeTask::class.java,
            SegmentedEncodeTask::class.java,
            ExtractSubtitleTask::class.java,

            FilePrepareForWorkTask::class.java,

            MediaReadTask::class.java,
            MetadataSearchTask::class.java,
            MigrateToContentStoreTask::class.java,

            StoreContentAndMetadataTask::class.java,
        )
    }
}