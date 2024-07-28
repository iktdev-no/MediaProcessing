package no.iktdev.mediaprocessing.shared.common.contract.dto

data class EventRequest(
    val file: String, // FullPath
    override val source: String
): Requester()