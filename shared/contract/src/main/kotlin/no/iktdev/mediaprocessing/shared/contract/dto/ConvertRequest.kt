package no.iktdev.mediaprocessing.shared.contract.dto

data class ConvertRequest(
    val file: String, // FullPath
    override val source: String
): Requester()