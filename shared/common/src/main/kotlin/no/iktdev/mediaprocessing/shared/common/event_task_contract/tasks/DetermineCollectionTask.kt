package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task

data class DetermineCollectionTask(
    val names: List<String>,
) : Task() {}