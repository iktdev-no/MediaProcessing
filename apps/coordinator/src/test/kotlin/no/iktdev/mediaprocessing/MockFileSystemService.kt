package no.iktdev.mediaprocessing

import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import java.io.File

class MockFileSystemService : FileSystemService {
    var copyShouldFail = false
    var identical = true
    val copied = mutableListOf<Pair<File, File>>()
    val deleted = mutableListOf<File>()

    override fun copy(source: File, destination: File): Boolean {
        copied += source to destination
        return !copyShouldFail
    }

    override fun areIdentical(a: File, b: File): Boolean {
        return identical
    }

    override fun delete(file: File) {
        deleted += file
    }
}