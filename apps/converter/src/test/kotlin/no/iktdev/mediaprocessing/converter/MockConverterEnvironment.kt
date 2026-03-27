package no.iktdev.mediaprocessing.converter

import no.iktdev.files.IFile
import no.iktdev.library.subtitle.reader.BaseReader

class MockConverterEnvironment(
    var canReadValue: Boolean = true,
    var reader: BaseReader? = null,
    var exporter: Exporter? = null
) : ConverterEnvironment {

    override fun canRead(file: IFile): Boolean = canReadValue

    override fun getReader(file: IFile): BaseReader? = reader

    override fun createExporter(input: IFile, outputDir: IFile, name: String): Exporter {
        return exporter ?: error("FakeEnv.exporter must be set before calling createExporter")
    }
}