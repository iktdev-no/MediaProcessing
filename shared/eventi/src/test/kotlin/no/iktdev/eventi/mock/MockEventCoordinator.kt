package no.iktdev.eventi.mock

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.implementations.ActiveMode
import no.iktdev.eventi.implementations.EventCoordinator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component


@Component
class MockEventCoordinator(
    @Autowired
    override var applicationContext: ApplicationContext,
    @Autowired
    override var eventManager: MockEventManager

) : EventCoordinator<EventImpl, MockEventManager>() {
    override fun getActiveTaskMode(): ActiveMode {
        return ActiveMode.Active
    }
}