import type { LineageNode } from "./types"
import type { Paginated, UiEvent, UiTask } from "./types"

export type PagedUiTask = Paginated<UiTask>


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


export interface EventQuery {
    key?: string[]
    referenceId?: string
    eventId?: string
    from?: string
    to?: string
    sort?: string
    order?: "ASC" | "DESC"
    eventTypes: string[] | undefined
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

export interface IgnoredTaskResponse {
    taskId: string
    referenceId: string,
    deletedEventId?: string,
    skippedEventId?: string,
    reset: boolean,
    resetAt: string
}

export interface LineageTreeNode extends LineageNode {
    children: LineageTreeNode[];
}
