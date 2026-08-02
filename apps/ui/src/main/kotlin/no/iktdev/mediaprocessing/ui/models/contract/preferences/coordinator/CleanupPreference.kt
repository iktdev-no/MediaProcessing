package no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator


data class CleanupPreference(
    val cacheCleanupPreference: CacheCleanupPreference,
    val inputCleanupPreference: InputCleanupPreference
)

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
