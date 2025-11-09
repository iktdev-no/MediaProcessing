package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event

data class CoverDownloadedEvent(
    val data: CoverDownloadedData
): Event() {
}

data class CoverDownloadedData(
    val outputFile: String
)