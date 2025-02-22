package no.iktdev.mediaprocessing.shared.common.contract.dto

import no.iktdev.mediaprocessing.shared.common.contract.ProcessType

data class EventRequest(
    val file: String, // FullPath
    override val source: String,
    val mode: ProcessType = ProcessType.MANUAL
): Requester()