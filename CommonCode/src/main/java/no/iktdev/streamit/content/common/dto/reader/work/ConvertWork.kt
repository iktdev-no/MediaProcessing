package no.iktdev.streamit.content.common.dto.reader.work

data class ConvertWork(
    override val collection: String,
    val language: String,
    override val inFile: String,
    override val outFile: String,
) : WorkBase(collection = collection, inFile = inFile, outFile = outFile)