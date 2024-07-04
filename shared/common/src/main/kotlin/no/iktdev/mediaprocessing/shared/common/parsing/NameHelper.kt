package no.iktdev.mediaprocessing.shared.common.parsing

import org.apache.commons.lang3.StringUtils
import java.text.Normalizer

object NameHelper {
    fun normalize(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
        val result = normalized.replace("\\p{M}".toRegex(), "")
        val cleaned = "[^A-Za-z0-9 -]".toRegex().replace(result, "")
        return StringUtils.stripAccents(cleaned)
    }
}