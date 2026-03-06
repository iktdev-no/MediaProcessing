package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.model.ContentExport
import no.iktdev.mediaprocessing.shared.common.model.ContentMigrationPlan

data class ContinuationSummaryEvent(
    val data: ContentExport,
    val plan: ContentMigrationPlan
): Event() {
}