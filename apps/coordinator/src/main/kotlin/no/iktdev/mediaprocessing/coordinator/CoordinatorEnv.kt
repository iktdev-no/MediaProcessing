package no.iktdev.mediaprocessing.coordinator

import no.iktdev.mediaprocessing.coordinator.config.ExecutablesConfig
import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths
import no.iktdev.mediaprocessing.shared.common.configs.StreamItConfig
import org.springframework.stereotype.Service
import java.io.File

@Service
class CoordinatorEnv(
    val streamIt: StreamItConfig,
    val exec: ExecutablesConfig,
    val media: MediaPaths
) {
    val streamitAddress = streamIt.address
    val ffprobe = exec.ffprobe

    val cachedContent = File(media.cache)
    val outgoingContent = File(media.outgoing)
    val incomingContent = File(media.incoming)
    val preference: File = File("/data/config/preference.json")

}
