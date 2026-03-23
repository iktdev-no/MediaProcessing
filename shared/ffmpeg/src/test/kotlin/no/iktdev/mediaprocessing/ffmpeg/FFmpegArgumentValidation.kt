package no.iktdev.mediaprocessing.ffmpeg

import org.junit.jupiter.api.Assertions
import kotlin.test.DefaultAsserter.fail

fun assertContainsAllWithOffset(
    expected: List<String>,
    actual: List<String>,
    dropStart: Int = 0,
    dropEnd: Int = 0
) {
    val slice = actual.drop(dropStart).dropLast(dropEnd)

    val missing = expected.filterNot { slice.contains(it) }
    val unexpected = slice.filterNot { expected.contains(it) }

    if (missing.isNotEmpty() || unexpected.isNotEmpty()) {
        val msg = buildString {
            appendLine("❌ assertContainsAllWithOffset failed")
            appendLine("Expected size: ${expected.size}, actual slice size: ${slice.size}")
            appendLine()

            if (missing.isNotEmpty()) {
                appendLine("Missing elements:")
                missing.forEach { appendLine("  - $it") }
                appendLine()
            }

            if (unexpected.isNotEmpty()) {
                appendLine("Unexpected elements in slice:")
                unexpected.forEach { appendLine("  + $it") }
                appendLine()
            }

            appendLine("Full slice:")
            appendLine(slice.joinToString(prefix = "[", postfix = "]"))
        }

        fail(msg)
    }

    // Size check
    Assertions.assertEquals(
        expected.size,
        slice.size,
        "Expected size ${expected.size} but was ${slice.size}. Slice: $slice"
    )
}
