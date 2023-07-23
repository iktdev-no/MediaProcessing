package no.iktdev.streamit.content.common.dto.reader.work

import java.util.*

data class ExtractWork(
    override val workId: String = UUID.randomUUID().toString(),
    override val collection: String,
    val language: String,
    override val inFile: String,
    val arguments: List<String>,
    override val outFile: String,
    var produceConvertEvent: Boolean = true
) : WorkBase(collection = collection, inFile = inFile, outFile = outFile)