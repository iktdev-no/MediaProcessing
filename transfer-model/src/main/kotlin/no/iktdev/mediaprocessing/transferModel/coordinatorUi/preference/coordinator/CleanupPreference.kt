package no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator

import java.time.Duration


data class CleanupPreference(
    val cacheCleanupPreference: CacheCleanupPreference,
    val inputCleanupPreference: InputCleanupPreference
) {
    companion object {
        fun default() = CleanupPreference(
            cacheCleanupPreference = CacheCleanupPreference(enabled = false, retention = Retention(1, RetentionUnit.Days), flows = FlowTypes.Auto),
            inputCleanupPreference = InputCleanupPreference(enabled = false, retention = Retention(7, RetentionUnit.Days), flows = FlowTypes.Auto)
        )
    }
}


data class CacheCleanupPreference(
    val enabled: Boolean,
    val retention: Retention = Retention(1, RetentionUnit.Days),
    val flows: FlowTypes = FlowTypes.Auto
)

data class InputCleanupPreference(
    val enabled: Boolean,
    val retention: Retention = Retention(7, RetentionUnit.Days),
    val flows: FlowTypes = FlowTypes.Auto
)


data class Retention(
    val value: Long,
    val unit: RetentionUnit
)

enum class RetentionUnit {
    Hours,
    Days
}

enum class FlowTypes {
    Auto,
    Manual,
    Any
}

fun Retention.toDuration(): Duration =
    when (unit) {
        RetentionUnit.Hours -> Duration.ofHours(value)
        RetentionUnit.Days -> Duration.ofDays(value)
    }


