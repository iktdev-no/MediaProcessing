package no.iktdev.mediaprocessing.ui.models.contract

import no.iktdev.mediaprocessing.shared.common.model.MediaType as SharedMediaType

enum class MediaType {
    Movie,
    Serie,
    Subtitle
}


fun SharedMediaType.toUiMediaType(): MediaType {
    return MediaType.valueOf(this.name)
}