package no.iktdev.mediaprocessing.coordinator

import mu.KotlinLogging
import no.iktdev.eventi.registry.EventTypeRegistry
import no.iktdev.eventi.registry.ProgressTypeRegistry
import no.iktdev.eventi.registry.TaskTypeRegistry
import no.iktdev.exfl.coroutines.CoroutinesDefault
import no.iktdev.exfl.coroutines.CoroutinesIO
import no.iktdev.exfl.observable.Observables
import no.iktdev.mediaprocessing.coordinator.config.AppConfig
import no.iktdev.mediaprocessing.coordinator.config.ExecutablesConfig
import no.iktdev.mediaprocessing.coordinator.config.ProcesserClientProperties
import no.iktdev.mediaprocessing.shared.common.configs.StreamItConfig
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.ProgressRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskRegistry
import no.iktdev.mediaprocessing.shared.common.getAppVersion
import no.iktdev.mediaprocessing.shared.database.DatabaseApplication
import no.iktdev.mediaprocessing.shared.database.DatabasebasedMediaProcessingApp
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@DatabasebasedMediaProcessingApp
class CoordinatorApplication: DatabaseApplication() {
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


    runApplication<CoordinatorApplication>(*args)
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
        ProgressTypeRegistry.register(ProgressRegistry.getProgresses())
    }
}

@Configuration
@EnableConfigurationProperties(
    value = [
        ExecutablesConfig::class,
        StreamItConfig::class,
        ProcesserClientProperties::class,
        AppConfig::class
    ]
)
class CoordinatorConfig