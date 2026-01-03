package no.iktdev.mediaprocessing.converter

import no.iktdev.library.subtitle.reader.BaseReader
import java.io.File

interface ConverterEnvironment {
    fun canRead(file: File): Boolean
    fun getReader(file: File): BaseReader?
    fun createExporter(input: File, outputDir: File, name: String): Exporter
}
