package no.iktdev.mediaprocessing.coordinator

import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.mediaprocessing.shared.contract.data.Event
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

class Coordinator(
    @Autowired
    override var applicationContext: ApplicationContext,
    @Autowired
    override var eventManager: EventsManager

) : EventCoordinator<Event, EventsManager>()
