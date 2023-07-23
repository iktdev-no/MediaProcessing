package no.iktdev.streamit.content.common.dto.reader.work

data class ExtractWork(
    override val collection: String,
    val language: String,
    override val inFile: String,
    val arguments: List<String>,
    override val outFile: String,
    var produceConvertEvent: Boolean = true
) : WorkBase(collection = collection, inFile = inFile, outFile = outFile)