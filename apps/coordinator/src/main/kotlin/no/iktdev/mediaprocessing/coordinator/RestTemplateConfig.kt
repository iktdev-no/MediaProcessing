package no.iktdev.mediaprocessing.coordinator

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {

    @Configuration
    class RestTemplateConfig(
        private val coordinatorEnv: CoordinatorEnv
    ) {

        @Bean
        fun streamitRestTemplate(): RestTemplate {
            return RestTemplateBuilder()
                .rootUri(coordinatorEnv.streamitAddress)
                .build()
        }
    }

}