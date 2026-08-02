package no.iktdev.mediaprocessing.ui.models.contract.requests

import no.iktdev.mediaprocessing.ui.models.contract.files.MediaActionType

data class StartProcessRequest(
    val fileUri: String,
    val mediaAction: List<MediaActionType>
) {
}