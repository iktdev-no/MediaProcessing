package no.iktdev.mediaprocessing.shared.common.sse

enum class SSEKeys(val key: String) {
    Ping("ping"),
    ProgressRestore("progress-restore"),
    Progress("progress"),
    HealthStatus("health-status"),

    ;
    companion object {
        fun fromKey(value: String): SSEKeys? {
            return entries.find { it.key == value }
        }
    }
}