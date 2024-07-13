package no.iktdev.eventi

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.implementations.EventsManagerImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.concurrent.TimeUnit

@ExtendWith(SpringExtension::class)
open class EventiImplementationBase: EventiApplicationTests() {

    @BeforeEach
    fun clearData() {
        coordinator!!.eventManager.events.clear()
    }

    @Autowired
    var eventManager: EventsManagerImpl<EventImpl>? = null

    @Test
    fun validateCoordinatorConstruction() {
        assertThat(eventManager).isNotNull()
        assertThat(eventManager?.dataSource).isNotNull()
        assertThat(coordinator).isNotNull()
        assertThat(coordinator?.eventManager?.dataSource).isNotNull()
    }

    private val timeout = 3_00000
    /**
     * @return true when
     */
    fun runPull(condition: () -> Boolean): Boolean {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeout) {
            if (condition()) {
                return true
            }
            TimeUnit.MILLISECONDS.sleep(500)
        }
        return condition()
    }

    fun getEvents(): List<List<EventImpl>> {
        return coordinator?.eventManager?.readAvailableEvents() ?: emptyList()
    }


}