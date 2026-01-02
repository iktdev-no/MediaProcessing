package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import io.github.classgraph.ClassGraph
import io.kotest.assertions.fail
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ValidateTasksRegistered {

    @Test
    fun validateTasksAreRegistered() {
        val tasksPackage = "no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks"
        val scanResult = ClassGraph()
            .acceptPackages(tasksPackage)
            .scan()

        val classesFound = scanResult.allClasses
            .map { it.loadClass() }
            .filter { Task::class.java.isAssignableFrom(it) }
            .toSet()

        val registered = TaskRegistry.getTasks().toSet()

        val missing = classesFound - registered
        val extra = registered - classesFound

        if (missing.isNotEmpty() || extra.isNotEmpty()) {
            fail(buildString {
                if (missing.isNotEmpty()) {
                    appendLine("Mangler i TaskRegistry:")
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