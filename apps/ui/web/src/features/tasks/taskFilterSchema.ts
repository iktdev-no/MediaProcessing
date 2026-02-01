export const taskFilterSchema = {
    status: ["Pending", "InProgress", "Completed", "Failed"],

    booleans: {
        claimed: ["claimed", "!claimed"],
        consumed: ["consumed", "!consumed"]
    },

    keyLabel: "Task name"
}

export const knownTaskNames: Array<string> = [
    "ConvertTask",
    "ExtractSubtitleTask",
    "EncodeTask",
    "CoverDownloadTask",
    "MediaReadTask",
    "MetadataSearchTask"
];