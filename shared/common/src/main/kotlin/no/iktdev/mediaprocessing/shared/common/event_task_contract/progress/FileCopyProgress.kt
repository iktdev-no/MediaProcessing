package no.iktdev.mediaprocessing.shared.common.event_task_contract.progress

import no.iktdev.eventi.models.Progress

class FileCopyProgress(override val progress: Int, val source: String, val destination: String, override val message: String = "") : Progress() {
}