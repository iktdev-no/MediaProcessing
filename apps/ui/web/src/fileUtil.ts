

export function canEncode(extension: string): boolean {
    const supported = [
        "mkv",
        "avi",
        "mp4",
        "wmv",
        "webm",
        "mov"
    ]
    const found = supported.find((item) => item === extension)
    return (found) ? true : false;
}

export function canExtract(extension: string): boolean {
    const supported = [
        "mkv"
    ]
    const found = supported.find((item) => item === extension)
    return (found) ? true : false;
}

export function canConvert(extension: string): boolean {
    const supported = [
        "ass",
        "srt",
        "vtt",
        "smi"
    ]
    const found = supported.find((item) => item === extension)
    return (found) ? true : false;
}