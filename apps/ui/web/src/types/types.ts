// AUTO-GENERATED. DO NOT EDIT.
// Source: no.iktdev.mediaprocessing.ui.dto

export interface UiTask {
  abandoned: boolean;
  activeOverrides: string[];
  availableOverrides: string[];
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

export interface ContinueSuccess {
  type: "ContinueSuccess";
}

export type ContinueResult = ContinueFailure | ContinueSuccess

export interface StartProcessRequest {
  fileUri: string;
  mediaAction: MediaActionType[];
}

export interface ContinueFailure {
  type: "ContinueFailure";
  message: string;
}

export interface DeleteRequest {
  uri: string;
}


export interface Paginated<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export type FileAccessMode = "READ_WRITE" | "READ_ONLY" | "NO_ACCESS"

export interface FileActions {
  fileActions: FileAction[];
  mediaActions: MediaAction[];
}

export type FileType = "Folder" | "File"

export type IUiFile = File | Folder

export interface File {
  type: "File";
  accessMode: FileAccessMode;
  actions: FileActions;
  created: number;
  extension: string;
  name: string;
  size: number;
  uri: string;
}

export interface MediaAction {
  id: MediaActionType;
  title: string;
}

export type MediaActionType = "All" | "Encode" | "ExtractSubtitles" | "ConvertSubtitle" | "MetadataSearch"

export interface FileAction {
  id: FileActionType;
  requiresConfirmation: boolean;
  title: string;
}

export interface Folder {
  type: "Folder";
  accessMode: FileAccessMode;
  actions: FileActions;
  created: number;
  name: string;
  uri: string;
}

export type FileActionType = "Open" | "Delete"


