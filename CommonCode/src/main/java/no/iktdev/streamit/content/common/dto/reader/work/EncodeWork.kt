package no.iktdev.streamit.content.common.dto.reader.work

data class EncodeWork(
    override val collection: String,
    override val inFile: String,
    override val outFile: String,
    val arguments: List<String>
) : WorkBase(collection = collection, inFile = inFile, outFile = outFile)