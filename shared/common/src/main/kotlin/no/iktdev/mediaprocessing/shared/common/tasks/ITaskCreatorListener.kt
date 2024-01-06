package no.iktdev.mediaprocessing.shared.common.tasks


interface ITaskCreatorListener<V> {
    fun  onEventReceived(referenceId: String, event: V, events: List<V>): Unit
}