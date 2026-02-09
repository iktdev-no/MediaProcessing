// AUTO-GENERATED. DO NOT EDIT.
// Source: no.iktdev.mediaprocessing.ui.dto

export interface UiTask {
  abandoned: boolean;
  claimed: boolean;
  claimedBy: string | null;
  consumed: boolean;
  data: string;
  elapsed: number | null;
  id: number;
  lastCheckIn: string | null;
  logFiles: string[];
  persistedAt: string;
  progress: number | null;
  referenceId: string;
  speed: number | null;
  status: string;
  task: string;
  taskId: string;
  timeLeft: number | null;
}

export interface SSEMessage {
  data: any;
  name: string;
}

export interface SystemStatus {
  converter: boolean;
  coordinatorRest: boolean;
  coordinatorSse: boolean;
  interval: number;
  processer: boolean;
  pyMetadata: boolean;
  pyWatcher: boolean;
  timestamp: number;
}

export interface UiEvent {
  data: string;
  event: string;
  eventId: string;
  id: number;
  persistedAt: string;
  referenceId: string;
}


export interface StartProcessRequest {
  fileUri: string;
  mediaAction: MediaActionType;
}


export interface Failure {
  message: string;
}

export interface DeleteRequest {
  uri: string;
}

export interface FfmpegDecodedProgress {
  duration: string;
  estimatedCompletion: string;
  estimatedCompletionSeconds: number;
  progress: number;
  speed: string;
  time: string;
}

export interface ProgressUpdate {
  message: string | null;
  progress: FfmpegDecodedProgress;
  referenceId: string;
  taskId: string;
}


export interface Paginated<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export interface FileActions {
  fileActions: FileAction[];
  mediaActions: MediaAction[];
}

export type FileType = "Folder" | "File"

export interface MediaAction {
  id: MediaActionType;
  title: string;
}

export type MediaActionType = "All" | "Encode" | "ExtractSubtitles" | "ConvertSubtitle" | "MetadataSearch"

export interface IFile {
  actions: FileActions;
  created: number;
  name: string;
  type: FileType;
  uri: string;
}

export interface FileAction {
  id: FileActionType;
  requiresConfirmation: boolean;
  title: string;
}

export interface FolderItem {
  actions: FileActions;
  created: number;
  name: string;
  type: FileType;
  uri: string;
}

export interface FileItem {
  actions: FileActions;
  created: number;
  extension: string;
  name: string;
  size: number;
  type: FileType;
  uri: string;
}

export type FileActionType = "Open" | "Delete"


