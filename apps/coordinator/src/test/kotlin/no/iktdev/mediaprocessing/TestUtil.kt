package no.iktdev.mediaprocessing

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Metadata
import no.iktdev.eventi.registry.EventListenerRegistry
import org.assertj.core.api.Assertions.assertThat
import java.lang.reflect.Field
import java.time.Instant

fun EventListenerRegistry.wipe() {
    val field: Field = EventListenerRegistry::class.java
        .superclass
        .getDeclaredField("listeners")
    field.isAccessible = true

    // Tøm map’en
    val mutableList = field.get(EventListenerRegistry) as MutableList<*>
    (mutableList as MutableList<Class<out EventListener>>).clear()

    // Verifiser at det er tomt
    assertThat(EventListenerRegistry.getListeners().isEmpty())
}

fun Event.withMetadata(metadata: Metadata): Event {
    val field = Event::class.java.getDeclaredField("metadata")
    field.isAccessible = true
    field.set(this, metadata)
    return this
}

fun Metadata.withCreated(instant: Instant): Metadata {
    val field = Metadata::class.java.getDeclaredField("created")
    field.isAccessible = true
    field.set(this, instant)
    return this
}

fun Event.withCreatedAt(instant: Instant): Event {
    val metadataField = Event::class.java.getDeclaredField("metadata")
    metadataField.isAccessible = true
    val metadata = metadataField.get(this)

    val createdField = metadata::class.java.getDeclaredField("created")
    createdField.isAccessible = true
    createdField.set(metadata, instant)

    return this
}
