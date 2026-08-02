package no.iktdev.mediaprocessing.ui

import mu.KotlinLogging
import no.iktdev.eventi.registry.EventTypeRegistry
import no.iktdev.eventi.registry.ProgressTypeRegistry
import no.iktdev.eventi.registry.TaskTypeRegistry
import no.iktdev.exfl.coroutines.CoroutinesDefault
import no.iktdev.exfl.coroutines.CoroutinesIO
import no.iktdev.exfl.observable.Observables
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.ProgressRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskRegistry
import no.iktdev.mediaprocessing.shared.common.getAppVersion
import no.iktdev.mediaprocessing.shared.database.DatabaseApplication
import no.iktdev.mediaprocessing.shared.database.DatabasebasedMediaProcessingApp
import no.iktdev.mediaprocessing.ui.client.MediaProcessingAppsProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

val log = KotlinLogging.logger {}


@DatabasebasedMediaProcessingApp
@EnableConfigurationProperties(
    AppsConfig::class,
    MediaConfig::class,
    MediaProcessingAppsProperties::class,
)
@EnableScheduling
class UIApplication: DatabaseApplication() {
}


val ioCoroutine = CoroutinesIO()
val defaultCoroutine = CoroutinesDefault()


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

    runApplication<UIApplication>(*args)
    log.info { "App Version: ${getAppVersion()}" }
}

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
