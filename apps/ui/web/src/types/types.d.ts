// AUTO-GENERATED. DO NOT EDIT.
// TSGenerator Version: 1.0-SNAPSHOT
// Time: 2026-08-11T12:03:35.477909188Z
// Source: no.iktdev.mediaprocessing.ui.models.contract


export interface UiTask {
  abandoned: boolean;
  activeOverrides: string[];
  availableOverrides: string[];
  claimed: boolean;
  claimedBy: string | null;
  consumed: boolean;
  data: string;
  elapsed: number | null;
  lastCheckIn: string | null;
  logFiles: string[];
  logs: string[];
  persistedAt: string;
  progress: number | null;
  referenceId: string;
  speed: number | null;
  status: string;
  task: string;
  taskId: string;
  timeLeft: number | null;
}

export type TaskStatus = "NotInitiated" | "Pending" | "InProgress" | "Completed" | "Failed" | "Cancelled" | "Skipped"

export type MediaType = "Movie" | "Serie" | "Subtitle"

export interface EventRate {
  lastFiveMinutes: number;
  lastMinute: number;
}

export interface UiEvent {
  data: string;
  derivedOf: string[] | null;
  event: string;
  eventId: string;
  persistedAt: string;
  referenceId: string;
}

export type SystemHealthStatus = "HEALTHY" | "DEGRADED" | "UNHEALTHY"

export interface StartProcessRequest {
  fileUri: string;
  mediaAction: MediaActionType[];
}

export interface DeleteRequest {
  uri: string;
}

export interface Response {
  message: string | null;
  success: boolean;
}


export interface Progress {
  progress: number;
  referenceId: string;
  taskId: string;
}

export type ProgressRef = EncodeProgress | FileCopyProgress | SimpleProgress

export interface FileCopyProgress extends Progress {
  type: "FileCopyProgress";
  destination: string;
  message: string;
  source: string;
}

export interface FfmpegDecodedProgress {
  duration: string;
  estimatedCompletion: string;
  estimatedCompletionSeconds: number;
  speed: string;
  time: string;
}

export interface EncodeProgress extends Progress {
  type: "EncodeProgress";
  additionalInfo: FfmpegDecodedProgress | null;
}

export interface SimpleProgress extends Progress {
  type: "SimpleProgress";
}

export interface LifecycleNode {
  event: UiEvent | null;
  lifecycleId: string;
  referenceId: string;
  taskOwnerEvent: UiEvent | null;
  tasks: TaskLifecycleItem[];
  title: string;
  type: LifecycleNodeType;
}

export type Mode = "Auto" | "Manual"

export type FailingReason = "MissingStart" | "NoRelevantTasksSet" | "TasksArePending" | "RequiredTasksHaveFailed" | "RequiredTasksAreNotComplete" | "RequiredOperationTasksHaveFailed"

export type ContextType = "Content" | "Metadata"

export interface EpisodeInfoSummary {
  episodeNumber: number;
  episodeTitle: string | null;
  seasonNumber: number;
}

export interface SequenceHealth {
  age: string;
  eventCount: number;
  expected: string;
  expectedFinishTime: string;
  isOverdue: boolean;
  lastEventAt: string;
  overdueDuration: string;
  referenceId: string;
  startTime: string;
}

export type SequenceActions = "Delete" | "Hold" | "Release"

export type LifecycleNodeType = "Event" | "EventTaskGroup"

export interface SequenceSummary {
  availableActions: SequenceActions[];
  collection: string | null;
  episodeInfo: EpisodeInfoSummary | null;
  failedTasks: string[];
  failingReasons: FailingReason | null;
  mediaType: MediaType | null;
  metadata: MetadataSummary | null;
  title: string | null;
}

export interface Sequence {
  availableActions: SequenceActions[];
  collection: string | null;
  currentState: CurrentState;
  hasErrors: boolean;
  inputFileName: string | null;
  lastEventId: string;
  lastEventTime: string;
  mediaType: MediaType | null;
  mode: Mode;
  referenceId: string;
  tasks: Record<TaskType, TaskStatus>;
  title: string | null;
  type: ContextType;
}

export interface TaskLifecycleItem {
  task: UiTask | null;
  taskId: string;
  taskResultEvents: UiEvent[];
}

export type TaskType = "ReadStreams" | "MetadataSearch" | "Encode" | "SubtitleExtract" | "SubtitleConvert" | "CoverDownload" | "ContentPersist" | "MediaInfoStored"

export interface MetadataSummary {
  alternativeTitles: string[];
  genres: string[];
  hasCover: boolean;
  source: string;
  title: string;
}

export type CurrentState = "Continuing" | "OnHold"

export interface SystemHealth {
  abandonedTaskIds: string[];
  abandonedTasks: number;
  activeTasks: number;
  details: Record<string, any | null>;
  failedTasks: number;
  lastActivity: string | null;
  overdueSequenceIds: string[];
  overdueSequences: SequenceHealth[];
  queuedTasks: number;
  sequencesOnHold: number;
  sequencesOnHoldIds: string[];
  stalledTaskIds: string[];
  stalledTasks: number;
  status: SystemHealthStatus;
}

export interface ProcessorPreference {
  cpuLimit: CPULimit;
}

export type Presets = "Ultrafast" | "Superfast" | "Veryfast" | "Faster" | "Fast" | "Medium" | "Slow" | "Slower" | "Veryslow" | "Placebo"

export interface AudioCodecConfig {
  application: OpusApplication | null;
  bitrate: number | null;
  channels: number | null;
  compressionLevel: number | null;
  profile: AacProfile | null;
  sampleRate: number | null;
  type: AudioCodecType;
}

export type FlowTypes = "Auto" | "Manual" | "Any"

export type AacProfile = "LC" | "HE" | "HEv2"

export interface InputCleanupPreference {
  enabled: boolean;
  flows: FlowTypes;
  retention: Retention;
}

export type AudioCodecType = "AAC" | "MP3" | "OPUS" | "VORBIS" | "FLAC" | "AC3" | "EAC3" | "DTS" | "PCM" | "COPY"

export type H264Profiles = "Baseline" | "Main" | "High" | "High10" | "High422" | "High444"

export interface CleanupPreference {
  cacheCleanupPreference: CacheCleanupPreference;
  inputCleanupPreference: InputCleanupPreference;
}

export interface VideoCodecConfig {
  bitrate: number | null;
  compressionLevel: number | null;
  cpuUsed: number | null;
  crf: number | null;
  level: number | null;
  preset: Presets | null;
  profile: H264Profiles | null;
  qscale: number | null;
  tune: string | null;
  type: VideoCodecType;
}

export interface MediaPreference {
  audioPreference: AudioPreference | null;
  videoPreference: VideoPreference | null;
}

export interface CacheCleanupPreference {
  enabled: boolean;
  flows: FlowTypes;
  retention: Retention;
}

export interface AudioPreference {
  default: AudioCodecConfig;
  extended: AudioCodecConfig | null;
}

export interface LanguagePreference {
  avoidDub: boolean;
  preferOriginal: boolean;
  preferredAudio: string[];
  preferredSubtitles: string[];
  subtitleFormatPriority: string[];
  subtitleSelectionMode: SubtitleSelectionMode;
}

export interface Retention {
  unit: RetentionUnit;
  value: number;
}

export type VideoCodecType = "HEVC" | "H264" | "VP9" | "VP8" | "AV1" | "VVC" | "XVID" | "RAW" | "COPY"

export interface VideoPreference {
  codec: VideoCodecConfig;
  enforceMkv: boolean;
}

export type RetentionUnit = "Hours" | "Days"

export type SubtitleSelectionMode = "DialogueOnly" | "DialogueAndForced" | "All"

export type OpusApplication = "Audio" | "Voip" | "LowDelay"

export interface CPULimit {
  enabled: boolean;
  limit: number;
}

export interface CoordinatorPreference {
  cleanup: CleanupPreference;
  language: LanguagePreference;
  media: MediaPreference;
}


export type FileAccessMode = "READ_WRITE" | "READ_ONLY" | "NO_ACCESS"

export interface FileActions {
  fileActions: FileAction[];
  mediaActions: MediaAction[];
}

export type FileType = "Folder" | "File"

export interface File extends UiFile {
  type: "File";
  extension: string;
  size: number;
}

export interface MediaAction {
  id: MediaActionType;
  title: string;
}


export interface UiFile {
  accessMode: FileAccessMode;
  actions: FileActions;
  created: number;
  name: string;
  type: FileType;
  uri: string;
}

export type UiFileRef = File | Folder

export type MediaActionType = "All" | "Encode" | "ExtractSubtitles" | "ExtractAndConvertSubtitles" | "ConvertSubtitle" | "MetadataSearch"

export interface FileAction {
  id: FileActionType;
  requiresConfirmation: boolean;
  title: string;
}

export interface Folder extends UiFile {
  type: "Folder";
}

export type FileActionType = "Open" | "Delete"

export interface PreservedFile {
  fileName: string;
  filePath: string;
  persistedAt: string | null;
  preserved: boolean;
  usedInReferences: string[];
}

export interface SSEHealthStatus extends SSEEvent {
  systemHealth: SystemStatus;
}

export interface Paginated<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export interface DiskInfo {
  device: string;
  freeBytes: number;
  mount: string;
  totalBytes: number;
  usedBytes: number;
  usedPercent: number;
}

export interface LineageNode {
  eventId: string;
  eventName: string;
  parents: string[];
  persistedAt: string | null;
}

export interface SystemStatus {
  converter: boolean;
  coordinatorRest: boolean;
  coordinatorSse: boolean;
  interval: number;
  processer: boolean;
  processerSse: boolean;
  pyMetadata: boolean;
  pyWatcher: boolean;
  timestamp: number;
}

