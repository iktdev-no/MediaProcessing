export type FileType = "Folder" | "File"

/* ──────────────────────────────────────────────
   Media Actions
   ────────────────────────────────────────────── */

export type MediaActionType =
    | "All"
    | "Encode"
    | "ExtractSubtitles"
    | "ConvertSubtitle"
    |

    export interface MediaAction {
    id: MediaActionType
    title: string
}

/* ──────────────────────────────────────────────
   File Actions
   ────────────────────────────────────────────── */

export type FileActionType =
    | "Open"
    | "Delete"

export interface FileAction {
    id: FileActionType
    title: string
    requiresConfirmation: boolean
}

/* ──────────────────────────────────────────────
   File Actions Container
   ────────────────────────────────────────────── */

export interface FileActions {
    mediaActions: MediaAction[]
    fileActions: FileAction[]
}

/* ──────────────────────────────────────────────
   Base File (sealed class equivalent)
   ────────────────────────────────────────────── */

export interface IFile {
    name: string
    uri: string
    created: number
    type: FileType
    actions: FileActions
}

/* ──────────────────────────────────────────────
   FileItem
   ────────────────────────────────────────────── */

export interface FileItem extends IFile {
    type: "File"
    extension: string
}

/* ──────────────────────────────────────────────
   FolderItem
   ────────────────────────────────────────────── */

export interface FolderItem extends IFile {
    type: "Folder"
}

/* ──────────────────────────────────────────────
   Union type for convenience
   ────────────────────────────────────────────── */

export type AnyFile = FileItem | FolderItem

export interface StartProcessRequest {
    fileUri: string
    mediaAction: MediaActionType
}
