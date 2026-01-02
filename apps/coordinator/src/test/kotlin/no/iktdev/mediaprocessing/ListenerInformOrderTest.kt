package no.iktdev.mediaprocessing

import no.iktdev.eventi.ListenerOrder
import no.iktdev.eventi.events.EventListenerRegistry
import no.iktdev.mediaprocessing.coordinator.CoordinatorApplication
import no.iktdev.mediaprocessing.coordinator.listeners.events.*
import no.iktdev.mediaprocessing.shared.common.config.DatasourceConfiguration
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension


@SpringBootTest(
    classes = [CoordinatorApplication::class,
        DatasourceConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = ["spring.flyway.enabled=true"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ComponentScan("no.iktdev.mediaprocessing.coordinator.listeners.events")
@ExtendWith(SpringExtension::class)
class ListenerInformOrderTest(): TestBase() {
    @Autowired lateinit var ctx: ApplicationContext

    @Test
    fun verifyTaskRegistryIsNotEmpty() {
        assertThat { TaskRegistry.getTasks().isNotEmpty() }
    }
    @Test
    fun verifyEventRegistryIsNotEmpty() {
        assertThat { EventRegistry.getEvents().isNotEmpty() }
    }


    @Test
    fun `only ordered handlers should be in correct order`() {
        val handlers = EventListenerRegistry.getListeners()
        assertThat(handlers).isNotEmpty
        val filtered = handlers.filter { it::class.java.isAnnotationPresent(ListenerOrder::class.java) }
        assertThat (filtered.map { it::class.simpleName }).containsExactly(
            StartedListener::class.simpleName,
            MediaParsedInfoListener::class.java.simpleName,
            MediaReadStreamsTaskCreatedListener::class.java.simpleName,
            MediaParseStreamsListener::class.java.simpleName,
            MediaCreateMetadataSearchTaskListener::class.java.simpleName,
        )
    }
}