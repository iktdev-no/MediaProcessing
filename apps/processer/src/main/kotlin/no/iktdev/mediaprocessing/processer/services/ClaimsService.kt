package no.iktdev.mediaprocessing.processer.services

import mu.KotlinLogging
import no.iktdev.mediaprocessing.processer.Coordinator
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
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
        val expiredClaims = PersistentDataReader().getExpiredClaimsProcessEvents()
        expiredClaims.forEach {
            log.info { "Found event with expired claim: ${it.referenceId}::${it.eventId}::${it.event}" }
        }
        val store = PersistentDataStore()
        expiredClaims.forEach {
            val result = store.releaseProcessEventClaim(referenceId = it.referenceId, eventId = it.eventId)
            if (result) {
                log.info { "Released claim on ${it.referenceId}::${it.eventId}::${it.event}" }
            } else {
                log.error { "Failed to release claim on ${it.referenceId}::${it.eventId}::${it.event}" }
            }
        }
        coordinator.readAllAvailableInQueue()
    }
}