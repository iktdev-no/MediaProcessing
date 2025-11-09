package no.iktdev.mediaprocessing.converter.convert

interface ConvertListener {
    fun onStarted(inputFile: String)
    fun onCompleted(inputFile: String, outputFiles: List<String>)
    fun onError(inputFile: String, message: String) {}
}