export interface Paginated<T> {
    items: T[]
    page: number      // 0-basert
    size: number
    total: number
}

export interface UiTask {
    id: number
    referenceId: string
    status: TaskStatus
    taskId: string
    task: string
    data: string
    claimed: boolean
    claimedBy?: string | null
    consumed: boolean
    lastCheckIn?: string | null
    persistedAt: string

    // Optional SSE fields
    progress?: number | null
    timeLeft?: number | null
    speed?: number | null
    elapsed?: number | null
}

export type PagedUiTask = Paginated<UiTask>

export type TaskStatus =
    | "Pending"
    | "InProgress"
    | "Completed"
    | "Failed"
    | "Cancelled"


export interface TaskQuery {
    key?: string[]
    status?: string[]
    claimed?: boolean
    consumed?: boolean
    referenceId?: string
    from?: string
    to?: string
    sort?: string
    order?: "ASC" | "DESC"
    page: number
    pageSize: number
}


export interface UiEvent {
    id: number
    referenceId: string
    eventId: string
    event: string
    data: string
    persistedAt: string
}


export interface EventQuery {
    key?: string[]
    referenceId?: string
    eventId?: string
    from?: string
    to?: string
    sort?: string
    order?: "ASC" | "DESC"
    page: number
    pageSize: number
}
export type PagedUiEvent = Paginated<UiEvent>

export interface ResetTaskResponse {
    taskId: string
    referenceId: string
    deletedEventId?: string
    reset: boolean
    resetAt: string
}

export type ContextType = "Content" | "Metadata"

export type Mode = "Auto" | "Manual"

export type CurrentState = "Continuing" | "OnHold"

export interface SequenceSummary {
    referenceId: string
    title: string
    inputFileName: string | null
    type: ContextType
    lastEventId: string
    lastEventTime: string
    readStreamsTaskStatus: TaskStatus
    metadataTaskStatus: TaskStatus
    encodeTaskStatus: TaskStatus
    extractTaskStatus: TaskStatus
    convertTaskStatus: TaskStatus
    coverDownloadTaskStatus: TaskStatus
    contentMigratedTaskStatus: TaskStatus,
    contentStoredTaskStatus: TaskStatus,
    mode: Mode
    currentState: CurrentState
    hasErrors: boolean
}

export type SequenceHealth = {
    referenceId: string

    age: string
    expected: string
    lastEventAt: string
    eventCount: number

    startTime: string
    expectedFinishTime: string
    overdueDuration: string
    isOverdue: boolean
}




export type CoordinatorHealth = {
    status: CoordinatorHealthStatus
    abandonedTasks: number
    stalledTasks: number
    activeTasks: number
    queuedTasks: number
    failedTasks: number
    sequencesOnHold: number
    lastActivity: string | null
    abandonedTaskIds: string[]
    stalledTaskIds: string[]
    overdueSequenceIds: string[]
    overdueSequences: SequenceHealth[]
    details: Record<string, unknown>
}



export type CoordinatorHealthStatus = 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY'


export type EventRate = {
    lastMinute: number
    lastFiveMinutes: number
}

export type DiskInfo = {
    mount: string
    device: string
    totalBytes: number
    freeBytes: number
    usedBytes: number
    usedPercent: number
}