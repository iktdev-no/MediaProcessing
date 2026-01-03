package no.iktdev.mediaprocessing.coordinator.util

import java.io.File

interface FileSystemService {
    fun copy(source: File, destination: File): Boolean
    fun areIdentical(a: File, b: File): Boolean
    fun delete(file: File)
}
