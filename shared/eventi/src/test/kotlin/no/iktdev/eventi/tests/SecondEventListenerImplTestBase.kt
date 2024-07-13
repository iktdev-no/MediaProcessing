package no.iktdev.eventi.tests

import no.iktdev.eventi.EventiImplementationBase
import no.iktdev.eventi.data.EventMetadata
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.mock.data.InitEvent
import no.iktdev.eventi.mock.data.SecondEvent
import no.iktdev.eventi.mock.listeners.FirstEventListener
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/*
class SecondEventListenerImplTestBase : EventiImplementationBase() {


    @BeforeEach
    fun validateCreationAndAccess() {
        assertThat(
            coordinator!!.getListeners()
                .find { it::class.simpleName == FirstEventListener::class.simpleName }).isNotNull()
        coordinator!!.eventManager.events.clear()
        coordinator!!.produceNewEvent(
            InitEvent(
                metadata = EventMetadata(
                    referenceId = "00000000-0000-0000-0000-000000000000",
                    status = EventStatus.Success
                ),
                data = "Init data"
            )
        )
    }

    @Test
    fun validate1() {
        runPull { getEvents().size > 2 }
        val events = coordinator?.eventManager?.readAvailableEvents() ?: emptyList()
        assertThat(events.filterIsInstance<SecondEvent>()).hasSize(2)
        assertThat(events.filterIsInstance<SecondEvent>().distinctBy { it.metadata.referenceId }).hasSize(2)
    }
}*/