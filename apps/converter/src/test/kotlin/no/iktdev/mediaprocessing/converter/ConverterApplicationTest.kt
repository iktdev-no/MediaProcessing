package no.iktdev.mediaprocessing.converter

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.AppTestBase
import no.iktdev.mediaprocessing.shared.database.config.DatasourceConfiguration
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import javax.sql.DataSource

@SpringBootTest(
    classes = [ConverterApplication::class,
        DatasourceConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = ["spring.flyway.enabled=true"])
@ExtendWith(SpringExtension::class)
class ConverterApplicationTest : AppTestBase() {

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

    @Test
    fun `ExposedInitializer should connect to database`() {
        mockkObject(Database)

        every {
            Database.connect(
                any<DataSource>(),
                any(),
                any(),
                any(),
                any()
            )
        } returns mockk()


        val context = SpringApplicationBuilder(ConverterApplication::class.java)
            .properties("spring.main.web-application-type=none")
            .run()

        verify(exactly = 1) { Database.connect(any<DataSource>(), any(), any(), any(), any()) }
    }


}
