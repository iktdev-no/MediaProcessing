package no.iktdev.mediaprocessing.ffmpeg


enum class Files(val fileName: String) {
    Output1("encodeProgress1.txt")
}


fun Files.getAsList(): List<String> {
    return this.javaClass.classLoader.getResource(this.fileName)?.readText()?.lines() ?: emptyList()
}


fun Files.getContent(): String? {
    return this.javaClass.classLoader.getResource(this.fileName)?.readText()
}
