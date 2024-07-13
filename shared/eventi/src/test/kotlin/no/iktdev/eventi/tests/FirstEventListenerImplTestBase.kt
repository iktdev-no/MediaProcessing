package no.iktdev.eventi.tests

import no.iktdev.eventi.EventiImplementationBase
import no.iktdev.eventi.data.EventMetadata
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.mock.data.FirstEvent
import no.iktdev.eventi.mock.data.InitEvent
import no.iktdev.eventi.mock.listeners.FirstEventListener
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
/*

class FirstEventListenerImplTestBase : EventiImplementationBase() {


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
        val events = coordinator?.eventManager?.readAvailableEvents() ?: emptyList()
        assertThat(events.any { it is FirstEvent }).isTrue()
    }

    @Test
    fun validate2() {
        coordinator!!.produceNewEvent(
            InitEvent(
                metadata = EventMetadata(
                    referenceId = "00000000-0000-0000-0000-000000000001",
                    status = EventStatus.Success
                ),
                data = "Init data"
            )
        )
        val events = coordinator?.eventManager?.readAvailableEvents() ?: emptyList()
        assertThat(events.filterIsInstance<FirstEvent>().distinctBy { it.metadata.referenceId }).hasSize(2)
    }
}*/