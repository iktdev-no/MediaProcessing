/**
 * This is only to run the code and verify behavior
 */

package no.iktdev.eventi

import no.iktdev.eventi.data.EventImpl
import no.iktdev.mediaprocessing.shared.common.datasource.DataSource
import no.iktdev.eventi.database.DatabaseConnectionConfig
import no.iktdev.eventi.implementations.EventListenerImpl
import no.iktdev.eventi.implementations.EventsManagerImpl
import no.iktdev.eventi.mock.MockEventManager
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component







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