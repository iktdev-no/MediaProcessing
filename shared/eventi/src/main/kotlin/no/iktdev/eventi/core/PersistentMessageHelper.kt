package no.iktdev.eventi.core

import no.iktdev.eventi.data.EventImpl

class PersistentMessageHelper<T: EventImpl>(val messages: List<T>) {

    fun findOrphanedEvents(): List<T> {
        val withDerivedId = messages.filter { it.metadata.derivedFromEventId != null }
        val idsFlat = messages.map { it.metadata.eventId }
        return withDerivedId.filter { it.metadata.derivedFromEventId !in idsFlat }
    }

    fun getEventsRelatedTo(eventId: String): List<T> {
        val triggered = messages.firstOrNull { it.metadata.eventId == eventId } ?: return emptyList()
        val usableEvents = messages.filter { it.metadata.eventId != eventId && it.metadata.derivedFromEventId != null }

        val derivedEventsMap = mutableMapOf<String, MutableList<String>>()
        for (event in usableEvents) {
            derivedEventsMap.getOrPut(event.metadata.derivedFromEventId!!) { mutableListOf() }.add(event.metadata.eventId)
        }
        val eventsToFind = mutableSetOf<String>()

        // Utfør DFS for å finne alle avledede hendelser som skal slettes
        dfs(triggered.metadata.eventId, derivedEventsMap, eventsToFind)

        return messages.filter { it.metadata.eventId in eventsToFind }
    }

    /**
     * @param eventId Initial eventId
     */
    private fun dfs(eventId: String, derivedEventsMap: Map<String, List<String>>, eventsToFind: MutableSet<String>) {
        eventsToFind.add(eventId)
        derivedEventsMap[eventId]?.forEach { derivedEventId ->
            dfs(derivedEventId, derivedEventsMap, eventsToFind)
        }
    }
}