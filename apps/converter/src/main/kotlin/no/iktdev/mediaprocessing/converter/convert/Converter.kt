package no.iktdev.mediaprocessing.converter.convert

import no.iktdev.files.IFile
import no.iktdev.library.subtitle.reader.BaseReader
import no.iktdev.mediaprocessing.converter.ConverterEnvironment
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask

abstract class Converter(val env: ConverterEnvironment, val listener: ConvertListener) {
    var writtenUris: List<String>? = null

    abstract fun getSubtitleReader(useFile: IFile): BaseReader?
    abstract suspend fun convert(data: ConvertTask.Data)

    class FileIsNullOrEmpty(override val message: String? = "File read is null or empty"): RuntimeException()
    class FileUnavailableException(override val message: String): RuntimeException()

    abstract fun getResult(): List<String>
}