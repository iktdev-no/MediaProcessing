package no.iktdev.mediaprocessing.shared.common.model.views

import no.iktdev.mediaprocessing.shared.common.model.MediaType

data class ParsedFileInfoView(
        val name: String,
        val collection: String,
        val mediaType: MediaType
    )