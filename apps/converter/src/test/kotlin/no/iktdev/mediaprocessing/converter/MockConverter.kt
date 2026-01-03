package no.iktdev.mediaprocessing.converter

import kotlinx.coroutines.delay
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.library.subtitle.reader.BaseReader
import no.iktdev.mediaprocessing.converter.convert.ConvertListener
import no.iktdev.mediaprocessing.converter.convert.Converter
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import java.io.File

class MockConverter(
    val delayMillis: Long = 0,
    private val simulatedResult: List<String>? =  null,
    private val taskResultStatus: TaskStatus = TaskStatus.Completed,
    private val throwException: Boolean = false,
    val mockEnv: ConverterEnvironment = MockConverterEnvironment(),
    listener: ConvertListener
) : Converter(env = mockEnv, listener = listener) {

    override fun getSubtitleReader(useFile: File): BaseReader? {
        TODO("Not yet implemented")
    }

    override suspend fun convert(data: ConvertTask.Data) {
        if (delayMillis > 0) delay(delayMillis)
        if (throwException) throw RuntimeException("Simulated convert failure")

        if (taskResultStatus == TaskStatus.Failed) {
            listener.onError(data.inputFile, "Failed state desired")
        } else {
            listener.onCompleted(data.inputFile, simulatedResult!!)
        }
    }

    override fun getResult(): List<String> {
        return simulatedResult!!
    }
}
