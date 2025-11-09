package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event


class MediaParsedInfoEvent(
    val data: ParsedData
): Event() {
}

data class ParsedData(
    val parsedTitle: String,
    val parsedFileName: String,
    val parsedSearchTitles: List<String>
) {
}