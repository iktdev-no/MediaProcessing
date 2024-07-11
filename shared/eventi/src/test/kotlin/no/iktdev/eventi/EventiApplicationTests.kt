package no.iktdev.eventi

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.eventi.mock.MockEventManager
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

@SpringBootTest(classes = [EventiApplication::class])
class EventiApplicationTests {

    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    var coordinator: EventCoordinator<EventImpl, MockEventManager>? = null

    @BeforeEach
    fun awaitCreationOfCoordinator() {

        runBlocking {
            while (coordinator?.isReady() != true) {
                delay(100)
            }
        }

    }

    @Test
    fun contextLoads() {
        Assertions.assertThat(coordinator?.getListeners()).isNotEmpty()
    }

}