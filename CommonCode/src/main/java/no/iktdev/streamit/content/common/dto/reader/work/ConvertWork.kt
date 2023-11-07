package no.iktdev.streamit.content.common.dto.reader.work

import java.util.*

data class ConvertWork(
    val workId: String = UUID.randomUUID().toString(),
    val collection: String,
    val language: String,
    val inFile: String,
    val outFiles: List<String>
)