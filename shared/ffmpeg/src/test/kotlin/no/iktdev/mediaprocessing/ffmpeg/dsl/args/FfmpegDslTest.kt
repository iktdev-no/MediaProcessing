package no.iktdev.mediaprocessing.ffmpeg.dsl.args

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FfmpegDslTest {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @Test
    @DisplayName("DSL → Instructions → JSON → Instructions → DSL should preserve build() output")
    @Disabled
    fun testDslRoundTrip() {

        // --- 1) Build original DSL ---
        val originalDsl = ffmpeg {
            input("/src/scratch/Potato.mkv") {
                    video(0) { map = true }
                    audio(0) { map = true }
                    subtitle(0) { map = true }
                }
            output("Potato.mp4") {
                overwrite = true
            }
        }

        val originalInstructions = originalDsl.toInstructions()
        val originalBuild = originalDsl.build()

        // --- 2) Serialize to JSON ---
        val json = gson.toJson(originalInstructions)

        // --- 3) Deserialize back ---
        val restoredInstructions =
            gson.fromJson(json, FFmpegInstructions::class.java)

        // --- 4) Build new DSL from restored instructions ---
        val restoredDsl = ffmpeg {
            fromInstructions(restoredInstructions)
        }

        val restoredBuild = restoredDsl.build()

        // --- 5) Compare ---
        assertEquals(
            originalBuild,
            restoredBuild,
            "Round‑trip DSL build() output should be identical"
        )
    }
}
