package no.iktdev.mediaprocessing.shared.common

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
abstract class TestBase {

    @LocalServerPort
    var port: Int = 0

    @Bean
    fun testRestTemplate(): TestRestTemplate {
        val baseUrl = URI("http://localhost:$port")
        return TestRestTemplate(RestTemplateBuilder().rootUri(baseUrl.toString()))
    }

    init {

    }


}