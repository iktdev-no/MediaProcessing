package no.iktdev.mediaprocessing.shared.common.contract

import no.iktdev.eventi.implementations.EventsManagerImpl
import no.iktdev.eventi.database.DataSource
import no.iktdev.mediaprocessing.shared.common.contract.data.Event

abstract class EventsManagerContract(dataSource: DataSource) : EventsManagerImpl<Event>(dataSource) {
}