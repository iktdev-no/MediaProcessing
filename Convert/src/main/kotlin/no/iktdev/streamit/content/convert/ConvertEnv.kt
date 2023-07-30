package no.iktdev.streamit.content.convert

class ConvertEnv {
    companion object {
        val allowOverwrite = System.getenv("ALLOW_OVERWRITE").toBoolean() ?: false
    }
}