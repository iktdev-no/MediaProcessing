package no.iktdev.mediaprocessing.ui.service

import no.iktdev.mediaprocessing.shared.common.database.cal.EventsManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class CompletedEventsService(
    @Autowired eventsManager: EventsManager
) {

}