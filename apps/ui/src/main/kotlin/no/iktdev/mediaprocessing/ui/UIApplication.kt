package no.iktdev.mediaprocessing.ui


import mu.KotlinLogging
import no.iktdev.eventi.database.MySqlDataSource
import no.iktdev.exfl.coroutines.CoroutinesDefault
import no.iktdev.exfl.coroutines.CoroutinesIO
import no.iktdev.exfl.observable.ObservableMap
import no.iktdev.exfl.observable.Observables
import no.iktdev.exfl.observable.observableMapOf
import no.iktdev.mediaprocessing.shared.common.DatabaseEnvConfig
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.database.EventsDatabase
import no.iktdev.mediaprocessing.shared.common.database.cal.EventsManager
import no.iktdev.mediaprocessing.shared.common.database.cal.RunnerManager
import no.iktdev.mediaprocessing.shared.common.database.cal.TasksManager
import no.iktdev.mediaprocessing.shared.common.getAppVersion
import no.iktdev.mediaprocessing.shared.common.toEventsDatabase
import no.iktdev.mediaprocessing.ui.dto.explore.ExplorerItem
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean

val log = KotlinLogging.logger {}
lateinit var eventDatabase: EventsDatabase
lateinit var eventsManager: EventsManager


@SpringBootApplication
class UIApplication {

    @Bean
    fun eventManager(): EventsManager {
        return eventsManager
    }
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

    eventDatabase = EventsDatabase().also {
        eventsManager = EventsManager(it.database)
    }


    runApplication<UIApplication>(*args)
    log.info { "App Version: ${getAppVersion()}" }
}



