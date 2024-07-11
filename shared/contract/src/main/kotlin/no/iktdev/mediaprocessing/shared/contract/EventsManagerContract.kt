package no.iktdev.mediaprocessing.shared.contract

import no.iktdev.eventi.implementations.EventsManagerImpl
import no.iktdev.mediaprocessing.shared.common.datasource.DataSource
import no.iktdev.mediaprocessing.shared.contract.data.Event

abstract class EventsManagerContract(dataSource: DataSource) : EventsManagerImpl<Event>(dataSource) {
}