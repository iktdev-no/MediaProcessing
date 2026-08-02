package no.iktdev.mediaprocessing.shared.common.projection.tasks

enum class TaskType {
    ReadMediaStreams,
    DownloadedCover,
    MetadataSearch,
    Encode,
    ExtractedSubtitles,
    ConvertedSubtitles,
    PrepareFileForWork,
    MigrateContent,
    PersistContentInfo,
    DetermineContentCollection
}