package no.iktdev.mediaprocessing.processer.services

import mu.KotlinLogging
import no.iktdev.mediaprocessing.processer.Coordinator
import no.iktdev.mediaprocessing.processer.eventManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class ClaimsService() {
    private val log = KotlinLogging.logger {}

    @Autowired
    lateinit var coordinator: Coordinator

    @Scheduled(fixedDelay = (300_000))
    fun validateClaims() {
        val expiredClaims = eventManager.getProcessEventsWithExpiredClaim()
        expiredClaims.forEach {
            log.info { "Found event with expired claim: ${it.referenceId}::${it.eventId}::${it.event}" }
        }
        expiredClaims.forEach {
            val result = eventManager.deleteProcessEventClaim(referenceId = it.referenceId, eventId = it.eventId)
            if (result) {
                log.info { "Released claim on ${it.referenceId}::${it.eventId}::${it.event}" }
            } else {
                log.error { "Failed to release claim on ${it.referenceId}::${it.eventId}::${it.event}" }
            }
        }
        coordinator.readAllAvailableInQueue()
    }
}