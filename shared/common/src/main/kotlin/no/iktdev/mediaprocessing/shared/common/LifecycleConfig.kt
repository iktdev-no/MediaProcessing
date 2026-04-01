package no.iktdev.mediaprocessing.shared.common

import no.iktdev.eventi.lifecycle.LifecycleStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LifecycleConfig {

    @Bean
    fun lifecycleStore(): LifecycleStore = LifecycleStore(50_000)
}
