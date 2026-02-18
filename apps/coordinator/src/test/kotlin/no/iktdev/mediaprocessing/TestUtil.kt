package no.iktdev.mediaprocessing

import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.registry.EventListenerRegistry
import org.assertj.core.api.Assertions.assertThat
import java.lang.reflect.Field

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