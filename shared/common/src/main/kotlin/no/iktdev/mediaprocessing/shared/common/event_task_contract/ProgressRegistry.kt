package no.iktdev.mediaprocessing.shared.common.event_task_contract

import no.iktdev.eventi.models.Progress
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.EncodeProgress
import no.iktdev.mediaprocessing.shared.common.event_task_contract.progress.FileCopyProgress

object ProgressRegistry {
     fun getProgresses(): List<Class<out Progress>> {
         return listOf(
             EncodeProgress::class.java,
             FileCopyProgress::class.java
         )
     }
}