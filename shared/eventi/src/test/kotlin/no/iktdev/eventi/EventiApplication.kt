/**
 * This is only to run the code and verify behavior
 */

package no.iktdev.eventi

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.implementations.EventsManagerImpl
import no.iktdev.eventi.mock.MockEventManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean


@SpringBootApplication
class EventiApplication {
    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Bean
    fun eventManager(): EventsManagerImpl<EventImpl> {
        return MockEventManager()
    }
}

fun main() {
    runApplication<EventiApplication>()
}