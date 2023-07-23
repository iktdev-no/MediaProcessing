package no.iktdev.streamit.content.common.dto.reader.work

import java.util.*

data class ConvertWork(
    override val workId: String = UUID.randomUUID().toString(),
    override val collection: String,
    val language: String,
    override val inFile: String,
    override val outFile: String,
) : WorkBase(collection = collection, inFile = inFile, outFile = outFile)