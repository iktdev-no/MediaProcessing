package no.iktdev.eventi.tests

import kotlinx.coroutines.runBlocking
import no.iktdev.eventi.EventiImplementationBase
import no.iktdev.eventi.data.EventMetadata
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.mock.data.ElementsToCreate
import no.iktdev.eventi.mock.data.InitEvent
import no.iktdev.eventi.mock.listeners.FirstEventListener
import no.iktdev.eventi.mock.listeners.ForthEventListener
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/*
class ForthEventListenerImplTestBase : EventiImplementationBase() {


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
    fun validate1(): Unit = runBlocking {
        runPull { getEvents().size > ElementsToCreate().elements.size *2 }
        val events = getEvents()
        assertThat(events.filter { it.eventType == ForthEventListener::class.java.simpleName }).hasSize(
            ElementsToCreate().elements.size)
    }
}*/