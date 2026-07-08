package no.iktdev.files

data class FileHash(val hash: String, val method: FileHashType) {
}

enum class FileHashType {
    SHA256,
    XX64Hash
}