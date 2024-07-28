package no.iktdev.mediaprocessing.shared.common.contract.dto

data class RequestWorkProceed(
    val referenceId: String,
    override val source: String
): Requester()
