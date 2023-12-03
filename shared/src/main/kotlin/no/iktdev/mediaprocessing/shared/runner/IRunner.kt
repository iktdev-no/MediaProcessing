package no.iktdev.mediaprocessing.shared.runner

interface IRunner {

    fun onStarted() {}

    fun onOutputChanged(line: String) {}

    fun onEnded() {}

    fun onError(code: Int)

}