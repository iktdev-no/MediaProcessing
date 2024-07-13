package no.iktdev.mediaprocessing.coordinator

import no.iktdev.mediaprocessing.shared.common.socket.SocketImplementation
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
class SocketLocalInit: SocketImplementation() {
}


