package no.iktdev.mediaprocessing.processer.runners

abstract class Runner {
    abstract suspend fun run(): RunnerResult<*>
}

sealed class RunnerResult<out T> {
    data class Success<T>(val payload: T) : RunnerResult<T>()
    data class Reject(val reason: String) : RunnerResult<Nothing>()
}
