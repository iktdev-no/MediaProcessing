package no.iktdev.streamit.content.common.deamon

interface IDaemon {

    fun onStarted() {}

    fun onOutputChanged(line: String) {}

    fun onEnded() {}

    fun onError(code: Int)

}