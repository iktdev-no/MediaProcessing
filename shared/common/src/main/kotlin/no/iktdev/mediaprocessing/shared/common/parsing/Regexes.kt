package no.iktdev.mediaprocessing.shared.common.parsing

object Regexes {
    val seasonAndEpisode = Regex("""(?i)\b(?:S|Season)\s*(\d+).*?(?:E|Episode)?\s*(\d+)\b""", RegexOption.IGNORE_CASE)
    val season = Regex("""(?i)\b(?:S|Season)\s*-?\s*(\d+)""", RegexOption.IGNORE_CASE)
}