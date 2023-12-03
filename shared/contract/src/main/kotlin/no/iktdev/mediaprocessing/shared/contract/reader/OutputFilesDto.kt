package no.iktdev.mediaprocessing.shared.contract.reader

class OutputFilesDto(
    val videoFile: String,
    val videoArguments: List<String>,
    val subtitleFiles: List<String>
)