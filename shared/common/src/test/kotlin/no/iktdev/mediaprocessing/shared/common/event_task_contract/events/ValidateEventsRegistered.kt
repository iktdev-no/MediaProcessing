package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import io.github.classgraph.ClassGraph
import io.kotest.assertions.fail
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ValidateEventsRegistered {

    @Test
    fun validateEventsAreRegistered() {
        val eventsPackage = "no.iktdev.mediaprocessing.shared.common.event_task_contract.events"
        val scanResult = ClassGraph()
            .acceptPackages(eventsPackage)
            .scan()

        val classesFound = scanResult.allClasses
            .map { it.loadClass() }
            .filter { Event::class.java.isAssignableFrom(it) }
            .toSet()

        val registered = EventRegistry.getEvents().toSet()

        val missing = classesFound - registered
        val extra = registered - classesFound

        if (missing.isNotEmpty() || extra.isNotEmpty()) {
            fail(buildString {
                if (missing.isNotEmpty()) {
                    appendLine("Mangler i EventRegistry:")
                    missing.forEach { appendLine(" - ${it.name}") }
                }
                if (extra.isNotEmpty()) {
                    appendLine("Registrert men finnes ikke i pakken:")
                    extra.forEach { appendLine(" - ${it.name}") }
                }
            })
        }
    }


}