package no.iktdev.mediaprocessing.shared.common

import no.iktdev.eventi.models.Event
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URI

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ExtendWith(SpringExtension::class)
abstract class AppTestBase {
    val history = mutableListOf<Event>()


    @LocalServerPort
    var port: Int = 0

    @Bean
    fun testRestTemplate(): TestRestTemplate {
        val baseUrl = URI("http://localhost:$port")
        return TestRestTemplate(RestTemplateBuilder().rootUri(baseUrl.toString()))
    }

    @BeforeEach
    open fun setup() {
        history.clear()
    }

    init {

    }

    fun Event.addToHistory(): Event {
        history.add(this)
        return this
    }


}