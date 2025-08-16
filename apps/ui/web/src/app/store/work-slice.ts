import { createSlice, PayloadAction } from "@reduxjs/toolkit"

export enum WorkStatus {
    Pending = "Pending",
    Started = "Started",
    Working = "Working",
    Completed = "Completed",
    Failed = "Failed"
}

export interface ProcesserProgress {
    progress: number
    speed: string
    timeWorkedOn: string
    timeLeft: string
}

export interface ProcesserEventInfo {
    referenceId: string
    eventId: string
    status: WorkStatus
    progress: ProcesserProgress
    inputFile: string
    outputFiles: Array<string>
}


export enum Status {
    Skipped = 'Skipped',
    Awaiting = 'Awaiting',
    NeedsApproval = 'NeedsApproval',
    Pending = 'Pending',
    InProgress = 'InProgress',
    Completed = 'Completed',
    Failed = "Failed"
}



export interface ContentEventState {
    referenceId: string
    title: string
    encode: Status
    extract: Status
    convert: Status
    completed: Status
    created: number
    encodeWork: ProcesserEventInfo
}

export interface ContentEventStateItems {
    items: Array<ContentEventState>,
    encodeWork: { [key: string]: ProcesserEventInfo }

}

const initialState: ContentEventStateItems = {
    items: [],
    encodeWork: {}
}

const workSlice = createSlice({
    name: "Work",
    initialState,
    reducers: {
        update(state, action: PayloadAction<Array<ContentEventState>>) {
            state.items = action.payload.map(item => ({
                ...item,
                rowId: item.referenceId, // Setter rowId lik referenceId
                encodeWork: state.encodeWork[item.referenceId] ?? undefined // Reapply encodeWork hvis det finnes
            }));
        },
        updateEncodeProgress(state, action: PayloadAction<ProcesserEventInfo>) {
            state.encodeWork[action.payload.referenceId] = action.payload;
            state.items = state.items.map(item =>
                item.referenceId === action.payload.referenceId
                    ? { ...item, encodeWork: action.payload }
                    : item
            );
        }
        
    }
})

export const { update, updateEncodeProgress } = workSlice.actions;
export default workSlice.reducer;