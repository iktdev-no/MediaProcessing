package no.iktdev.mediaprocessing.coordinator.reader
/*
import com.google.gson.Gson
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.streamit.library.kafka.dto.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import java.io.File

class BaseInfoFromFileTest {

    @Autowired
    private lateinit var testBase: KafkaTestBase

    @Mock
    lateinit var coordinatorProducer: CoordinatorProducer



    val baseInfoFromFile = BaseInfoFromFile(coordinatorProducer)

    @Test
    fun testReadFileInfo() {
        val input = ProcessStarted(
            Status.COMPLETED, ProcessType.FLOW,
            File("/var/cache/[POTATO] Kage no Jitsuryokusha ni Naritakute! S2 - 01 [h265].mkv").absolutePath
        )

        val result = baseInfoFromFile.readFileInfo(input)
        assertThat(result).isInstanceOf(BaseInfoPerformed::class.java)
        val asResult = result as BaseInfoPerformed
        assertThat(result.status).isEqualTo(Status.COMPLETED)
        assertThat(asResult.title).isEqualTo("Kage no Jitsuryokusha ni Naritakute!")
        assertThat(asResult.sanitizedName).isEqualTo("Kage no Jitsuryokusha ni Naritakute! S2 - 01")
    }

    @ParameterizedTest
    @MethodSource("names")
    fun test(data: TestInfo) {
        val gson = Gson()
        val result = baseInfoFromFile.readFileInfo(data.input)
        JSONAssert.assertEquals(
            data.expected,
            gson.toJson(result),
            false
        )
    }

    data class TestInfo(
        val input: ProcessStarted,
        val expected: String
    )

    companion object {
        @JvmStatic
        private fun names(): List<Named<TestInfo>> {
            return listOf(
                Named.of(
                    "Potato", TestInfo(
                        ProcessStarted(
                            Status.COMPLETED, ProcessType.FLOW,
                            "E:\\input\\Top Clown Findout.1080p.H264.AAC5.1.mkv"
                        ),
                        """
                        {
                            "status": "COMPLETED",
                            "title": "Top Clown Findout",
                            "sanitizedName": "Top Clown Findout"
                        }
                        """.trimIndent()
                    )
                ),
                Named.of("Filename with UHD wild tag", TestInfo(
                    ProcessStarted(
                        Status.COMPLETED, ProcessType.FLOW,
                        "E:\\input\\Wicked.Potato.Chapter.1.2023.UHD.BluRay.2160p.DDP.7.1.DV.HDR.x265.mp4"
                    ),
                    """
                        {
                            "status": "COMPLETED",
                            "title": "Wicked Potato Chapter 1",
                            "sanitizedName": "Wicked Potato Chapter 1"
                        }
                    """.trimIndent()
                )
                )
            )
        }
    }

}*/