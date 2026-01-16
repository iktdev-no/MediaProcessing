package no.iktdev.mediaprocessing.converter

import mu.KotlinLogging
import no.iktdev.eventi.events.EventTypeRegistry
import no.iktdev.eventi.tasks.TaskTypeRegistry
import no.iktdev.exfl.coroutines.CoroutinesDefault
import no.iktdev.exfl.coroutines.CoroutinesIO
import no.iktdev.exfl.observable.Observables
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskRegistry
import no.iktdev.mediaprocessing.shared.common.getAppVersion
import no.iktdev.mediaprocessing.shared.database.DatabaseApplication
import no.iktdev.mediaprocessing.shared.database.DatabasebasedMediaProcessingApp
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration

@DatabasebasedMediaProcessingApp
open class ConverterApplication: DatabaseApplication() {
}

val ioCoroutine = CoroutinesIO()
val defaultCoroutine = CoroutinesDefault()

private val log = KotlinLogging.logger {}

fun main(args: Array<String>) {
    ioCoroutine.addListener(listener = object: Observables.ObservableValue.ValueListener<Throwable> {
        override fun onUpdated(value: Throwable) {
            value.printStackTrace()
        }
    })
    defaultCoroutine.addListener(listener = object: Observables.ObservableValue.ValueListener<Throwable> {
        override fun onUpdated(value: Throwable) {
            value.printStackTrace()
        }
    })


    runApplication<ConverterApplication>(*args)
    log.info { "App Version: ${getAppVersion()}" }
}
//private val logger = KotlinLogging.logger {}

@Configuration
open class ApplicationConfiguration() {
    init {
        EventRegistry.getEvents().let {
            EventTypeRegistry.register(it)
        }
        TaskRegistry.getTasks().let {
            TaskTypeRegistry.register(it)
        }
    }
}
