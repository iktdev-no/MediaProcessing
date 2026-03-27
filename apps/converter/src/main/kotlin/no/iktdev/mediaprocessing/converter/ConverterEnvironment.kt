package no.iktdev.mediaprocessing.converter

import no.iktdev.files.IFile
import no.iktdev.library.subtitle.reader.BaseReader

interface ConverterEnvironment {
    fun canRead(file: IFile): Boolean
    fun getReader(file: IFile): BaseReader?
    fun createExporter(input: IFile, outputDir: IFile, name: String): Exporter
}
