// AUTO-GENERATED. DO NOT EDIT.
// Source: no.iktdev.mediaprocessing.transferModel.coordinatorUi

export interface CoordinatorHealth {
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
  status: CoordinatorHealthStatus;
}

export interface DeleteResultFailure {
  type: "DeleteResultFailure";
  message: string;
}


export type Presets = "Ultrafast" | "Superfast" | "Veryfast" | "Faster" | "Fast" | "Medium" | "Slow" | "Slower" | "Veryslow" | "Placebo"



export type H264Profiles = "Baseline" | "Main" | "High" | "High10" | "High422" | "High444"

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

export type VideoCodecType = "HEVC" | "H264" | "VP9" | "VP8" | "AV1" | "VVC" | "XVID" | "RAW" | "COPY"

export interface PreferenceConfig {
  language: LanguagePreference;
  processer: ProcesserPreference;
}

export interface AudioCodecConfig {
  application: OpusApplication | null;
  bitrate: number | null;
  channels: number | null;
  compressionLevel: number | null;
  profile: AacProfile | null;
  sampleRate: number | null;
  type: AudioCodecType;
}

export type AacProfile = "LC" | "HE" | "HEv2"


export type AudioCodecType = "AAC" | "MP3" | "OPUS" | "VORBIS" | "FLAC" | "AC3" | "EAC3" | "DTS" | "PCM" | "COPY"


export interface AudioPreference {
  default: AudioCodecConfig;
  extended: AudioCodecConfig | null;
}

export type OpusApplication = "Audio" | "Voip" | "LowDelay"

export interface LanguagePreference {
  avoidDub: boolean;
  preferOriginal: boolean;
  preferredAudio: string[];
  preferredSubtitles: string[];
  subtitleFormatPriority: string[];
  subtitleSelectionMode: SubtitleSelectionMode;
}


export interface VideoPreference {
  codec: VideoCodecConfig;
  enforceMkv: boolean;
}

export interface ProcesserPreference {
  audioPreference: AudioPreference | null;
  videoPreference: VideoPreference | null;
}

export type SubtitleSelectionMode = "DialogueOnly" | "DialogueAndForced" | "All"

export type TaskStatus = "NotInitiated" | "Pending" | "InProgress" | "Completed" | "Failed" | "Cancelled"

export type Mode = "Auto" | "Manual"

export type DeleteResult = DeleteResultFailure | DeleteResultSuccess

export type CoordinatorHealthStatus = "HEALTHY" | "DEGRADED" | "UNHEALTHY"

export type ContextType = "Content" | "Metadata"

export interface EventRate {
  lastFiveMinutes: number;
  lastMinute: number;
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

export interface DeleteResultSuccess {
  type: "DeleteResultSuccess";
}

export interface CoordinatorTaskDto {
  abandoned: boolean;
  claimed: boolean;
  claimedBy: string | null;
  consumed: boolean;
  data: string;
  id: number;
  lastCheckIn: string | null;
  logs: string[];
  persistedAt: string;
  referenceId: string;
  status: string;
  task: string;
  taskId: string;
}

export interface CoordinatorEventDto {
  data: string;
  event: string;
  eventId: string;
  id: number;
  persistedAt: string;
  referenceId: string;
}

export interface SequenceEvent {
  eventId: string;
  metadata: MetadataDto;
  payload: Record<string, any | null> | null;
  referenceId: string;
  timestamp: string;
  type: string;
}

export interface SequenceSummary {
  contentMigratedTaskStatus: TaskStatus;
  contentStoredTaskStatus: TaskStatus;
  convertTaskStatus: TaskStatus;
  coverDownloadTaskStatus: TaskStatus;
  currentState: CurrentState;
  encodeTaskStatus: TaskStatus;
  extractTaskStatus: TaskStatus;
  hasErrors: boolean;
  inputFileName: string | null;
  lastEventId: string;
  lastEventTime: string;
  metadataTaskStatus: TaskStatus;
  mode: Mode;
  readStreamsTaskStatus: TaskStatus;
  referenceId: string;
  title: string;
  type: ContextType;
}

export interface MetadataDto {
  createdAt: string;
  derivedFromEventIds: string[] | null;
}

export interface ApiResponse {
  message: string;
  ok: boolean;
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

export type CurrentState = "Continuing" | "OnHold"

