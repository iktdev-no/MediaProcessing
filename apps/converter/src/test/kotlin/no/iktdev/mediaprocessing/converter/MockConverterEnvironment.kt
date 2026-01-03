package no.iktdev.mediaprocessing.converter

import no.iktdev.library.subtitle.reader.BaseReader
import java.io.File

class MockConverterEnvironment(
    var canReadValue: Boolean = true,
    var reader: BaseReader? = null,
    var exporter: Exporter? = null
) : ConverterEnvironment {

    override fun canRead(file: File): Boolean = canReadValue

    override fun getReader(file: File): BaseReader? = reader

    override fun createExporter(input: File, outputDir: File, name: String): Exporter {
        return exporter ?: error("FakeEnv.exporter must be set before calling createExporter")
    }
}