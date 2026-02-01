package no.iktdev.mediaprocessing.ui

import mu.KotlinLogging
import no.iktdev.exfl.coroutines.CoroutinesDefault
import no.iktdev.exfl.coroutines.CoroutinesIO
import no.iktdev.exfl.observable.Observables
import no.iktdev.mediaprocessing.shared.common.getAppVersion
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

val log = KotlinLogging.logger {}


@SpringBootApplication
@EnableConfigurationProperties(AppsConfig::class, MediaConfig::class)
@EnableScheduling
class UIApplication {
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

