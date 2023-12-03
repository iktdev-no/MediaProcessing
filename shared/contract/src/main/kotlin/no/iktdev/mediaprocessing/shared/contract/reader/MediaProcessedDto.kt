package no.iktdev.mediaprocessing.shared.contract.reader

import no.iktdev.mediaprocessing.shared.contract.ProcessType

data class MediaProcessedDto(
    val referenceId: String,
    val process: ProcessType?,
    val inputFile: String?,
    val metadata: MetadataDto?,
    val outputFiles: OutputFilesDto?
)