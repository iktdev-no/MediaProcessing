package no.iktdev.mediaprocessing.coordinator.reader

import TestKafka
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.streamit.library.kafka.dto.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

class BaseInfoFromFileTest {
    val referenceId = UUID.randomUUID().toString()
    val baseInfoFromFile = BaseInfoFromFile(TestKafka.producer, TestKafka.listener)

    @Test
    fun testReadFileInfo() {
        val input = ProcessStarted(Status.COMPLETED, ProcessType.FLOW,
            File("/var/cache/[POTATO] Kage no Jitsuryokusha ni Naritakute! S2 - 01 [h265].mkv").absolutePath
        )

        val result = baseInfoFromFile.readFileInfo(input)
        assertThat(result).isInstanceOf(BaseInfoPerformed::class.java)
        val asResult = result as BaseInfoPerformed
        assertThat(result.status).isEqualTo(Status.COMPLETED)
        assertThat(asResult.title).isEqualTo("Kage no Jitsuryokusha ni Naritakute!")
        assertThat(asResult.sanitizedName).isEqualTo("Kage no Jitsuryokusha ni Naritakute! S2 - 01")
    }



}