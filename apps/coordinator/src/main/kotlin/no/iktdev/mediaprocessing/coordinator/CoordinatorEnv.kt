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

    val scratchFolder = File(media.scratch)
    val intermediateFolder  = File(media.intermediate)
    val outboxFolder = File(media.outbox)
    val inboxFolder = File(media.inbox)
    val preference: File = File("/data/config/preference.json")

}
