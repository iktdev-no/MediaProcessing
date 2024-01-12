package no.iktdev.mediaprocessing.shared.contract.reader


data class OutputFilesDto(
    val video: String?, // FullName (Path + name)
    val subtitles: List<String> = emptyList() // (Path + Name)
)
