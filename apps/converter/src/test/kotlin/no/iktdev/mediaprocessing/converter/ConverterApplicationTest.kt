package no.iktdev.mediaprocessing.converter

import io.mockk.junit5.MockKExtension
import mu.KotlinLogging
import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.stores.TaskStore
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest(classes = [ConverterApplication::class])
@ExtendWith(SpringExtension::class)
class ConverterApplicationTest {
    private val log = KotlinLogging.logger {}

    data class TestTask(
        val success: Boolean
    ) : Task()


    @Test
    fun `context loads and common configuration is available`() {
        // Hvis du har beans du vil verifisere, kan du autowire dem her
        // @Autowired lateinit var database: Database

        // Dummy assertion for å verifisere at konteksten starter
        assertNotNull(Unit)
    }


    @Test
    fun `Verify that we can access TaskStore`() {
        val tasks = TaskStore.getPendingTasks()
        assertNotNull(tasks)
        assert(tasks.isEmpty())

        TaskStore.persist(TestTask(success = true).newReferenceId())

        val tasksAfter = TaskStore.getPendingTasks()
        assertNotNull(tasksAfter)
        assert(tasksAfter.isNotEmpty())
    }
}
