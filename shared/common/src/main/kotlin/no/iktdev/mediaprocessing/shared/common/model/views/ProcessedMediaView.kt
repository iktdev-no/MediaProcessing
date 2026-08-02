package no.iktdev.mediaprocessing.shared.common.model.views

import no.iktdev.files.IFile

data class ProcessedMediaView(
    val encodedFile: IFile?,
    val extractedFiles: List<IFile>,
    val convertedFiles: List<IFile>
    )