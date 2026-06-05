package no.iktdev.mediaprocessing.ui

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class CoordinatorHealthCheck(
    @param:Qualifier("coordinatorWebClient") private val webClient: WebClient,
) : ApplicationRunner {

    private val log = KotlinLogging.logger {}

    override fun run(args: ApplicationArguments?) {
        webClient.get()
            .uri("/actuator/health")
            .retrieve()
            .bodyToMono(String::class.java)
            .doOnNext { log.info { "Coordinator is reachable" } }
            .doOnError { log.error(it) { "Coordinator is NOT reachable" } }
            .subscribe()
    }
}
