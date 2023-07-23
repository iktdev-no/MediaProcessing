package no.iktdev.streamit.content.common.dto.reader.work

import java.util.*

data class EncodeWork(
    override val workId: String = UUID.randomUUID().toString(),
    override val collection: String,
    override val inFile: String,
    override val outFile: String,
    val arguments: List<String>
) : WorkBase(collection = collection, inFile = inFile, outFile = outFile)