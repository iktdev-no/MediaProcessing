package no.iktdev.mediaprocessing.converter

import no.iktdev.files.FakeFile
import no.iktdev.files.IFile
import org.junit.jupiter.api.BeforeAll

open class TestBase(): no.iktdev.mediaprocessing.shared.common.TestBase() {

    fun IFile.asFake(): FakeFile? = this as? FakeFile

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupIFileFactory(): Unit {
            IFile.factory = { path -> FakeFile(path, exists = true) }
        }
    }

}