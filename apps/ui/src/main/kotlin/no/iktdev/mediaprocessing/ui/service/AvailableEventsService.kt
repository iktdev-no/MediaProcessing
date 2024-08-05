package no.iktdev.mediaprocessing.ui.service

import no.iktdev.mediaprocessing.shared.common.database.cal.EventsManager
import no.iktdev.mediaprocessing.ui.eventsManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class AvailableEventsService(
    @Autowired eventsManager: EventsManager
) {

    fun pullAvailableEvents() {
        eventsManager.readAvailableEvents().onEach {

        }
    }


}