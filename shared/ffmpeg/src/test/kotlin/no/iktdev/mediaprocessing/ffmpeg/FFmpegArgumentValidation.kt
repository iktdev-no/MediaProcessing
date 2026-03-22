package no.iktdev.mediaprocessing.ffmpeg

import org.junit.jupiter.api.Assertions

fun assertContainsAllWithOffset(
    expected: List<String>,
    actual: List<String>,
    dropStart: Int = 0,
    dropEnd: Int = 0
) {
    val slice = actual.drop(dropStart).dropLast(dropEnd)
    // Sjekk at alle forventede elementer finnes i slice (uavhengig av rekkefølge)
    for (exp in expected) {
        Assertions.assertTrue(
            slice.contains(exp),
            "Expected element '$exp' not found in actual slice: $slice"
        )
    }
    // Sjekk at størrelsen matcher
    Assertions.assertEquals(
        expected.size,
        slice.size,
        "Expected size ${expected.size} but was ${slice.size}. Slice: $slice"
    )
}