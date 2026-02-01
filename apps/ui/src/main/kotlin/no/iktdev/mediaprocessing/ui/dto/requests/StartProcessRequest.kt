package no.iktdev.mediaprocessing.ui.dto.requests

import no.iktdev.mediaprocessing.ui.dto.file.MediaActionType

data class StartProcessRequest(
    val fileUri: String,
    val mediaAction: MediaActionType
) {
}