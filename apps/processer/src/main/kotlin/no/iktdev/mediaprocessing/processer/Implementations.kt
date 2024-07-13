package no.iktdev.mediaprocessing.processer

import no.iktdev.mediaprocessing.shared.common.Defaults
import no.iktdev.mediaprocessing.shared.common.socket.SocketImplementation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestTemplate



@Configuration
public class DefaultProcesserConfiguration: Defaults() {
}

@Configuration
class SocketImplemented: SocketImplementation() {
}