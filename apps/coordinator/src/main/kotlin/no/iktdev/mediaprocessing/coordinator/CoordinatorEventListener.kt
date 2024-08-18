package no.iktdev.mediaprocessing.coordinator

import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.EventsListenerContract
import no.iktdev.mediaprocessing.shared.common.database.cal.EventsManager

abstract class CoordinatorEventListener(): EventsListenerContract<EventsManager, Coordinator>() {
    abstract override val produceEvent: Events
    abstract override val listensForEvents: List<Events>
    abstract override var coordinator: Coordinator?
}
