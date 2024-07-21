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

    fun cleanup(input: String): String {
        var cleaned = Regex("(?<=\\w)[_.](?=\\w)").replace(input, " ")
        cleaned = Regexes.illegalCharacters.replace(cleaned, " - ")
        cleaned = Regexes.trimWhiteSpaces.replace(cleaned, " ")
        return NameHelper.normalize(cleaned)
    }
}

fun String.isCharOnlyUpperCase(): Boolean {
    return "[^A-Za-z]".toRegex().replace(this, "").all { it.isUpperCase() }
}