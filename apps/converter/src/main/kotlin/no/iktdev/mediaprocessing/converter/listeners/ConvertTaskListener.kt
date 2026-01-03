package no.iktdev.mediaprocessing.converter.listeners

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.library.subtitle.export.Export
import no.iktdev.library.subtitle.reader.BaseReader
import no.iktdev.library.subtitle.reader.Reader
import no.iktdev.mediaprocessing.converter.ConverterEnvironment
import no.iktdev.mediaprocessing.converter.ExportAdapter
import no.iktdev.mediaprocessing.converter.Exporter
import no.iktdev.mediaprocessing.converter.convert.ConvertListener
import no.iktdev.mediaprocessing.converter.convert.Converter
import no.iktdev.mediaprocessing.converter.convert.Converter2
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ConvertTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import org.springframework.stereotype.Component
import java.io.File
import java.util.*

@Component
open class ConvertTaskListener(): TaskListener(TaskType.CPU_INTENSIVE) {

    override fun getWorkerId(): String {
        return "${this::class.java.simpleName}-${TaskType.CPU_INTENSIVE}-${UUID.randomUUID()}"
    }

    override fun supports(task: Task): Boolean {
        return task is ConvertTask
    }

    override suspend fun onTask(task: Task): Event? {
        if (task !is ConvertTask) {
            throw IllegalArgumentException("Invalid task type: ${task::class.java.name}")
        }
        val converter = getConverter()

        withHeartbeatRunner {
            reporter?.updateLastSeen(task.taskId)
        }

        withContext(Dispatchers.Unconfined) {
            converter.convert(task.data)
        }

        return try {
            val result = converter.getResult()
            val newEvent = ConvertTaskResultEvent(
                data = ConvertTaskResultEvent.ConvertedData(
                    language = task.data.language,
                    outputFiles = result,
                    baseName = task.data.outputFileName
                ),
                status = TaskStatus.Completed
            ).producedFrom(task)
            newEvent
        } catch (e: Exception) {
            e.printStackTrace()
            val newEvent = ConvertTaskResultEvent(
                data = null,
                status = TaskStatus.Failed
            ).producedFrom(task)
            newEvent
        }
    }

    open fun getConverter(): Converter {
        return Converter2(getConverterEnvironment(), getListener())
    }

    open fun getListener(): ConvertListener {
        return DefaultConvertListener()
    }

    class DefaultConvertListener: ConvertListener {
        override fun onStarted(inputFile: String) {
        }

        override fun onCompleted(inputFile: String, outputFiles: List<String>) {
        }
    }

    open fun getConverterEnvironment(): ConverterEnvironment {
        return DefaultConverterEnvironment()
    }

    class DefaultConverterEnvironment : ConverterEnvironment {
        override fun canRead(file: File) = file.canRead()

        override fun getReader(file: File): BaseReader? =
            Reader(file).getSubtitleReader()

        override fun createExporter(input: File, outputDir: File, name: String): Exporter {
            return ExportAdapter(Export(input, outputDir, name))
        }

    }

}